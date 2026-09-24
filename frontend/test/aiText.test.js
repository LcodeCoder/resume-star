import test from 'node:test'
import assert from 'node:assert/strict'
import { visibleAiText } from '../src/utils/aiText.js'

test('removes completed and unfinished reasoning before interview display or speech', () => {
  assert.equal(visibleAiText('<think>internal</think>请介绍项目经历。'), '请介绍项目经历。')
  assert.equal(visibleAiText('<think>The user wants me to'), '')
  assert.equal(visibleAiText('请回答。<analysis>do not show'), '请回答。')
  assert.equal(visibleAiText('正常正文'), '正常正文')
})
