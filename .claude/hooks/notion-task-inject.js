#!/usr/bin/env node
/**
 * UserPromptSubmit hook — інжектує контекст активної Notion-задачі (In progress)
 * у системний промпт Claude Code при наявності trigger-слів у запиті.
 *
 * Вимагає: NOTION_SKILLS_TOKEN (або NOTION_TOKEN) у env або .env файлі.
 * Завжди завершується з exit(0) — помилки не блокують роботу.
 */

const https = require('https');
const fs = require('fs');
const path = require('path');

// Database ID дошки Spovishun (з ancestor-path задачі)
const DATABASE_ID = '3193462f68a980d69ec9c7ccc6329b88';
const NOTION_VERSION = '2022-06-28';

const TRIGGER_WORDS = [
  // English
  'implement', 'fix', 'add', 'create', 'write', 'build', 'update',
  'refactor', 'debug', 'test', 'make', 'change', 'develop',
  // Ukrainian
  'реалізу', 'виправ', 'додай', 'створи', 'напиш', 'зроби', 'зміни',
  'розроби', 'задача', 'таск', 'фіч'
];

function loadToken() {
  if (process.env.NOTION_SKILLS_TOKEN) return process.env.NOTION_SKILLS_TOKEN;
  if (process.env.NOTION_TOKEN) return process.env.NOTION_TOKEN;

  const envPath = path.join(process.cwd(), '.env');
  try {
    const content = fs.readFileSync(envPath, 'utf8');
    const tokenMatch = content.match(/^NOTION_TOKEN=(.+)$/m);
    if (tokenMatch) return tokenMatch[1].trim();
    const skillsMatch = content.match(/^NOTION_SKILLS_TOKEN=(.+)$/m);
    if (skillsMatch) return skillsMatch[1].trim();
  } catch {
    // .env not found
  }
  return null;
}

function notionRequest(token, method, apiPath, body) {
  return new Promise((resolve, reject) => {
    const bodyStr = body ? JSON.stringify(body) : null;
    const options = {
      hostname: 'api.notion.com',
      path: apiPath,
      method,
      headers: {
        'Authorization': `Bearer ${token}`,
        'Notion-Version': NOTION_VERSION,
        'Content-Type': 'application/json',
        ...(bodyStr && { 'Content-Length': Buffer.byteLength(bodyStr) })
      }
    };

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try { resolve(JSON.parse(data)); } catch { resolve(null); }
      });
    });

    req.on('error', reject);
    if (bodyStr) req.write(bodyStr);
    req.end();
  });
}

function extractBlocks(blocks) {
  const lines = [];

  for (const block of blocks) {
    const type = block.type;
    const content = block[type];
    if (!content) continue;

    const text = (content.rich_text || []).map(rt => rt.plain_text).join('');

    if (type.startsWith('heading_')) {
      if (text) lines.push(`\n**${text}**`);
    } else if (type === 'paragraph') {
      if (text) lines.push(text);
    } else if (type === 'bulleted_list_item' || type === 'numbered_list_item') {
      if (text) lines.push(`- ${text}`);
    } else if (type === 'quote') {
      if (text) lines.push(`> ${text}`);
    }
    // toggle (🤖 prompt) — пропускаємо навмисно
  }

  return lines.join('\n').trim();
}

async function main() {
  let raw = '';
  process.stdin.setEncoding('utf8');
  for await (const chunk of process.stdin) raw += chunk;

  let data;
  try { data = JSON.parse(raw); } catch { process.exit(0); }

  const prompt = (data.prompt || '').toLowerCase();
  const hasTrigger = TRIGGER_WORDS.some(word => prompt.includes(word));
  if (!hasTrigger) process.exit(0);

  const token = loadToken();
  if (!token) {
    process.stderr.write('[notion-task-inject] NOTION_SKILLS_TOKEN not set, skipping\n');
    process.exit(0);
  }

  try {
    const queryResult = await notionRequest(token, 'POST', `/v1/databases/${DATABASE_ID}/query`, {
      filter: { property: 'Status', status: { equals: 'In progress' } },
      page_size: 1
    });

    const page = queryResult?.results?.[0];
    if (!page) process.exit(0);

    const name = (page.properties?.Name?.title || []).map(t => t.plain_text).join('') || 'Unknown';
    const pageId = page.id.replace(/-/g, '');

    const blocksResult = await notionRequest(token, 'GET', `/v1/blocks/${pageId}/children?page_size=100`, null);
    const blocks = (blocksResult?.results || []).filter(b => b.type !== 'toggle');
    const content = extractBlocks(blocks);

    const systemPrompt = [
      '## 🪝 Active Task (Notion — In progress)',
      `**${name}**`,
      '',
      content,
      '',
      '---',
      '*Work within the scope of this task. Do not go beyond what is described.*'
    ].join('\n');

    process.stdout.write(JSON.stringify({ systemPrompt }));
    process.exit(0);

  } catch (err) {
    process.stderr.write(`[notion-task-inject] Error: ${err.message}\n`);
    process.exit(0);
  }
}

main();