---
navigation:
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
  position: 316
  title: AE胶乳提取加工机
  icon: applied_more_machines:ae_latex_extraction_processor
item_ids:
  - applied_more_machines:ae_latex_extraction_processor
---

# AE胶乳提取加工机

<BlockImage id="applied_more_machines:ae_latex_extraction_processor" scale="6" />

这台机器将整根原木提取成胶乳，并可同时把胶乳与水加工成干橡胶。

## 库存与进度

左下格只接收 `minecraft:logs` 标签内的原木，右格只输出产物。
两个流体槽分别只收胶乳与水各 16 B，流体槽可用鼠标携带的单个流体容器点击灌入。
提取使用世界实际加载的工业先锋流体提取机配方，非默认配方优先。

## 模式
- **被动**：自动从 网络里 找原木与水，产出的胶乳一半送入 网络，一半留槽加工。
- **胶乳**：手动供原木，胶乳全部送入 网络。
- **橡胶**：手动供料，会禁用从槽内向 网络 或管道抽出胶乳，全部用于制作干橡胶。