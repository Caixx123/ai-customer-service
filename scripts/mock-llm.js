/**
 * Local OpenAI-compatible mock for plumbing/UI verification only.
 * It returns fixed tool calls and fixed text; it is NOT a real model.
 *
 * Terminal 1: node scripts/mock-llm.js
 * Terminal 2: AI_BASE_URL=http://127.0.0.1:8899 AI_API_KEY=mock AI_MODEL=mock-model mvn spring-boot:run
 */
const http = require('http');

function sse(res, chunks) {
  res.writeHead(200, {
    'Content-Type': 'text/event-stream; charset=utf-8',
    'Cache-Control': 'no-cache',
    'Connection': 'keep-alive'
  });
  for (const chunk of chunks) {
    res.write('data: ' + JSON.stringify(chunk) + '\n\n');
  }
  res.write('data: [DONE]\n\n');
  res.end();
}

function baseChunk(delta, finishReason) {
  return {
    id: 'chatcmpl-mock',
    object: 'chat.completion.chunk',
    created: Math.floor(Date.now() / 1000),
    model: 'mock-model',
    choices: [{ index: 0, delta: delta, finish_reason: finishReason || null }]
  };
}

function textResponse(res, text) {
  sse(res, [
    baseChunk({ role: 'assistant', content: text }, null),
    baseChunk({}, 'stop')
  ]);
}

function toolCall(res, name, args) {
  sse(res, [
    baseChunk({
      role: 'assistant',
      tool_calls: [{
        index: 0,
        id: 'call_' + Date.now(),
        type: 'function',
        function: { name: name, arguments: JSON.stringify(args) }
      }]
    }, null),
    baseChunk({}, 'tool_calls')
  ]);
}

function userText(messages) {
  const user = [...messages].reverse().find(m => m.role === 'user');
  if (!user) return '';
  if (typeof user.content === 'string') return user.content;
  if (Array.isArray(user.content)) {
    return user.content.map(part => part.text || '').join(' ');
  }
  return '';
}

function hasToolResult(messages) {
  return messages.some(m => m.role === 'tool');
}

const server = http.createServer((req, res) => {
  let body = '';
  req.on('data', chunk => body += chunk);
  req.on('end', () => {
    let payload = {};
    try { payload = JSON.parse(body); } catch (_) { /* ignore */ }
    const messages = payload.messages || [];
    const system = messages.find(m => m.role === 'system');
    const systemText = system && typeof system.content === 'string' ? system.content : '';
    const user = userText(messages);
    const toolResult = hasToolResult(messages);

    console.log('REQ', req.method, req.url,
      '| system=', systemText.slice(0, 24),
      '| user=', user.slice(0, 40),
      '| toolResult=', toolResult,
      '| tools=', (payload.tools || []).map(t => t.function && t.function.name).join(','));

    if (systemText.includes('电商订单售后专家')) {
      if (!toolResult) return toolCall(res, 'list_my_orders', {});
      return textResponse(res, '你最近有 4 笔订单：O20261023001 已签收，O20261023002 退款审核中，O20261023003 运输中，O20261023005 待发货。');
    }

    if (systemText.includes('电商售后政策专家')) {
      if (!toolResult) return toolCall(res, 'search_knowledge_base', { query: user });
      return textResponse(res, '自签收之日起 7 个自然日内，商品完好、不影响二次销售可无理由退货；定制、鲜活易腐等商品除外。【来源：退货退款政策.md · 七天无理由退货】');
    }

    if (toolResult) {
      if (user.includes('投诉')) {
        return textResponse(res, '已为你创建转人工工单，客服会在 24 小时内联系你。');
      }
      if (user.includes('7天') || user.includes('无理由') || user.includes('价保')) {
        return textResponse(res, '自签收之日起 7 个自然日内，商品完好、不影响二次销售可无理由退货；定制、鲜活易腐等商品除外。【来源：退货退款政策.md · 七天无理由退货】');
      }
      return textResponse(res, '你最近有 4 笔订单：O20261023001 已签收，O20261023002 退款审核中，O20261023003 运输中，O20261023005 待发货。');
    }

    if (user.includes('投诉')) {
      return toolCall(res, 'handoff_to_human', {
        reason: '用户投诉',
        summary: '用户反馈商品质量问题并要求投诉，需要人工跟进。'
      });
    }
    if (user.includes('7天') || user.includes('无理由') || user.includes('价保')) {
      return toolCall(res, 'policy_expert', { query: user });
    }
    return toolCall(res, 'order_expert', { query: user || '查询当前用户最近的订单' });
  });
});

server.listen(8899, '127.0.0.1', () => {
  console.log('Mock LLM listening on http://127.0.0.1:8899');
});
