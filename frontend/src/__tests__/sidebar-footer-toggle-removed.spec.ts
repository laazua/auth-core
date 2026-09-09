import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const sidebarPath = resolve(__dirname, '../components/Sidebar.vue')
const headerPath = resolve(__dirname, '../components/Header.vue')

const sidebarContent = readFileSync(sidebarPath, 'utf-8')
const headerContent = readFileSync(headerPath, 'utf-8')

function extractTemplate(content: string): string {
  const match = content.match(/<template>([\s\S]*?)<\/template>/)
  return match ? match[1] : ''
}

function extractScript(content: string): string {
  const match = content.match(/<script[^>]*>([\s\S]*?)<\/script>/)
  return match ? match[1] : ''
}

function extractStyle(content: string): string {
  const match = content.match(/<style[^>]*>([\s\S]*?)<\/style>/)
  return match ? match[1] : ''
}

describe('SidebarFooterToggleRemovedSpec', () => {
  describe('footerToggleRemoved', () => {
    it('Sidebar.vue 模板不包含 .sidebar__footer 类名', () => {
      const template = extractTemplate(sidebarContent)
      expect(template).not.toContain('sidebar__footer')
    })

    it('Sidebar.vue 模板不包含 toggleCollapse 方法调用', () => {
      const template = extractTemplate(sidebarContent)
      expect(template).not.toContain('toggleCollapse')
    })
  })

  describe('footerStylesRemoved', () => {
    it('Sidebar.vue 样式不包含 .sidebar__footer 选择器', () => {
      const style = extractStyle(sidebarContent)
      expect(style).not.toContain('.sidebar__footer')
    })

    it('Sidebar.vue 样式不包含 .sidebar__toggle 选择器', () => {
      const style = extractStyle(sidebarContent)
      expect(style).not.toContain('.sidebar__toggle')
    })
  })

  describe('headerTogglePreserved', () => {
    it('Header.vue 模板包含 .header__toggle 类名', () => {
      const template = extractTemplate(headerContent)
      expect(template).toContain('header__toggle')
    })

    it('Header.vue 模板包含 @click="handleSidebarToggle"', () => {
      const template = extractTemplate(headerContent)
      expect(template).toContain('@click="handleSidebarToggle"')
    })

    it('Header.vue 脚本中 handleSidebarToggle 调用 emit("toggle")', () => {
      const script = extractScript(headerContent)
      expect(script).toContain("emit('toggle')")
    })
  })
})