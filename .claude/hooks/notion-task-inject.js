#!/usr/bin/env node
/**
 * UserPromptSubmit hook — інжектує контекст активної Notion-задачі (In progress)
 * у системний промпт Claude Code при наявності trigger-слів у запиті.
 *
 * Якщо промпт містить "start task" / "почати задачу" / "нова задача" —
 * також створює git-гілку від develop.
 *
 * Вимагає: NOTION_SKILLS_TOKEN (або NOTION_TOKEN) у env або .env файлі.
 * Завжди завершується з exit(0) — помилки не блокують роботу.
 */

const https = require('https');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

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

const START_TASK_TRIGGERS = [
  'start new task', 'start task', 'start branch', 'checkout task',
  'почати задачу', 'нова задача', 'беру задачу', 'починаю задачу',
  'створи гілку', 'нова гілка', 'перейди на гілку'
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

function richText(blocks) {
  return (blocks || []).map(rt => rt.plain_text).join('').trim();
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

function extractBranchFromBlocks(blocks) {
  for (let i = 0; i < blocks.length; i++) {
    const block = blocks[i];
    const type = block.type;
    if (!type) continue;

    const content = block[type];
    if (!content) continue;

    const text = richText(content.rich_text);

    if (text.includes('Branch name') || text.includes('🌿')) {
      for (let j = i + 1; j < blocks.length && j <= i + 3; j++) {
        const next = blocks[j];
        if (!next || !next.type) continue;
        const nextContent = next[next.type];
        if (!nextContent) continue;
        const nextText = richText(nextContent.rich_text);
        if (nextText && nextText.startsWith('feature/')) {
          return nextText.trim();
        }
      }
    }

    if (text.startsWith('feature/spovishun-')) {
      return text.trim();
    }
  }
  return null;
}

function gitCheckoutFromDevelop(branch) {
  try {
    execSync('git checkout develop', { stdio: 'pipe' });
    execSync('git pull origin develop', { stdio: 'pipe' });
    execSync(`git checkout -b "${branch}"`, { stdio: 'pipe' });
    return { ok: true, message: `Created and switched to: ${branch}` };
  } catch (err) {
    return { ok: false, message: err.message };
  }
}

async function main() {
  let raw = '';
  process.stdin.setEncoding('utf8');
  for await (const chunk of process.stdin) raw += chunk;

  let data;
  try { data = JSON.parse(raw); } catch { process.exit(0); }

  const prompt = (data.prompt || '').toLowerCase();

  const isStartTask = START_TASK_TRIGGERS.some(t => prompt.includes(t));
  const hasTrigger = isStartTask || TRIGGER_WORDS.some(word => prompt.includes(word));

  if (!hasTrigger) process.exit(0);

  const token = loadToken();
  if (!token) {
    process.stderr.write('[notion-task-inject] NOTION_SKILLS_TOKEN not set, skipping\n');
    process.exit(0);
  }

  try {
    const queryResult = await notionRequest(token, 'POST', `/v1/databases/${DATABASE_ID}/query`, {
      filter: { property: 'Status', status: { equals: 'To do' } },
      page_size: 1
    });

    const page = queryResult?.results?.[0];
    if (!page) process.exit(0);

    const name = (page.properties?.Name?.title || []).map(t => t.plain_text).join('') || 'Unknown';
    const pageId = page.id.replace(/-/g, '');

    const blocksResult = await notionRequest(token, 'GET', `/v1/blocks/${pageId}/children?page_size=100`, null);
    const allBlocks = blocksResult?.results || [];
    const contentBlocks = allBlocks.filter(b => b.type !== 'toggle');
    const content = extractBlocks(contentBlocks);

    let branchNote = '';

    if (isStartTask) {
      let branch = extractBranchFromBlocks(allBlocks);

      // Fallback: derive from title
      if (!branch) {
        const numMatch = name.match(/spovishun-(\d+)/i);
        if (numMatch) {
          const taskNum = numMatch[1];
          const slug = name
            .replace(/^feature\/spovishun-\d+:\s*/i, '')
            .toLowerCase()
            .replace(/[^a-z0-9\s-]/g, '')
            .trim()
            .replace(/\s+/g, '-')
            .replace(/-+/g, '-')
            .split('-').slice(0, 3).join('-');
          branch = `feature/spovishun-${taskNum}-${slug}`;
        }
      }

      if (branch) {
        const result = gitCheckoutFromDevelop(branch);
        branchNote = result.ok
          ? `\n**Git:** ${result.message}`
          : `\n**Git error:** ${result.message}`;
      }
    }

    const systemPrompt = [
      '## 🪝 Active Task (Notion — In progress)',
      `**${name}**`,
      '',
      content,
      branchNote,
      '',
      '---',
      '*Work within the scope of this task. Do not go beyond what is described.*'
    ].filter(l => l !== undefined).join('\n');

    process.stdout.write(JSON.stringify({ additionalContext: systemPrompt }));
    process.exit(0);

  } catch (err) {
    process.stderr.write(`[notion-task-inject] Error: ${err.message}\n`);
    process.exit(0);
  }
}

main();