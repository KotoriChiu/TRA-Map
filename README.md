# TRA-map 開發方向與目標說明

---
## 概要
本專案是基於[公共運輸整合資訊平台(PTX)](https://ptx.transportdata.tw/PTX)的服務而構思出來的，目的是能讓使用者更方便更快速找尋自己需要搭乘的列車(不過本開發者我 腦子要炸了)
## 開發方向

- 圖形介面化
- 地圖上會有各種車種的圖示存在(自強 區間 普悠瑪 太魯閣...等)
- 點擊該列車 會顯示該車次的資料
- 每兩分種更新一次資料(防止GET次數超過限制)

## 開發進度

    目前開發進度緩速前進中，目前只搞定了API資料的抓取 資料整理，之後要處理的項目大概會有(新增車站陣列 將所有資料中的每一站的資料存進各自的車站陣列 順逆行分開處理 以表定進站、離站時間判斷列車目前位置..等(很緩慢就對了))

## 相關連結
- [公共運輸整合資訊流通服務平台(PTX)](https://ptx.transportdata.tw/PTX)
- [PTX提供之範例程式](https://github.com/ptxmotc/Sample-code)

