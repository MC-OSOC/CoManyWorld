# co-many-world
![ข้อความอธิบายภาพ](backgrounds-01.png)

co-many-world เป็นปลั๊กอินสำหรับเซิร์ฟเวอร์ **Spigot และ Paper** Minecraft 1.21 ขึ้นไป (รวมถึง 26.x) ที่ช่วยให้คุณสามารถสร้างและจัดการหลายโลก
#### นี่คือปลั๊กอินตัวอย่างทดลองใช้งานและทดสอบเท่านั้น ไม่แนะนำให้ไปใช้กับ server จริง เพราะปลั๊กอินนี้อยู่ในช่วงพัฒนา และอาจจะไม่สมบูรณ์

[English version](https://github.com/MC-OSOC/co-many-world/blob/main/README-EN.md)
## คุณสมบัติ

- สร้างโลกใหม่ได้ง่ายดายด้วยคำสั่ง
- วาร์ปไปยังโลกต่าง ๆ ได้อย่างรวดเร็ว
- ลบโลกที่ไม่ต้องการ
- แสดงรายชื่อโลกที่สร้างไว้
- นำเข้าโลกจากไฟล์ ZIP หรือโฟลเดอร์
- สำรองข้อมูลโลก
- รองรับ EssentialsX
- รองรับทั้ง Spigot และ Paper ด้วยไฟล์ .jar เดียวกัน (บน Paper จะใช้การวาร์ปแบบ async ไม่ทำให้เซิร์ฟเวอร์ค้าง)

## การติดตั้ง

1. ดาวน์โหลดไฟล์จาก release
2. นำไฟล์ .jar ไปไว้ในโฟลเดอร์ `plugins` ของเซิร์ฟเวอร์ Minecraft (Spigot หรือ Paper, ต้องใช้ Java 21 ขึ้นไป)

### Build เอง
ต้องใช้ JDK 21 และ Maven
```bash
mvn package
```
ไฟล์จะอยู่ที่ `target/CoManyWorld-<version>.jar` ใช้ได้ทั้ง Spigot และ Paper
ถ้าต้องการตรวจว่าโค้ด compile กับ Paper API ได้ และรันชุดทดสอบ (MockBukkit) ให้ใช้ `mvn package -Ppaper`

การออก Release ผ่าน GitHub Actions: ดู [docs/RELEASING.md](docs/RELEASING.md)

## คำสั่ง

#### สร้างโลกใหม่
`/co-many create <worldName> [-11|-12|-13|-all]`
- `-11` : โลกปกติ (ค่าเริ่มต้น ถ้าไม่ใส่ประเภท จะใช้ `default-world-type` ใน `config.yml`)
- `-12` : โลกนรก (Nether)
- `-13` : โลก The End
- `-all`: สร้างโลกทั้งหมด
- `-s` : Seed

#### คำสั่งพื้นฐาน
- **วาร์ปไปยังโลก**: `/co-many tp <worldName>`
- **แสดงรายชื่อโลก**: `/co-many list`
- **ลบโลก** (ย้ายไปถังขยะ ยังไม่ลบไฟล์): `/co-many delete <worldName>` หรือทั้งกลุ่ม `/co-many delete many_world/<name>/`
- **นำเข้าโลก**: `/co-many import <worldName>`
- **สำรองข้อมูลโลก**: `/co-many backup <worldName>`
- **ลบโลกถาวร (โลกที่อยู่ในถังขยะ)**: `/co-many clear` เพื่อดูรายการ แล้ว `/co-many clear confirm` เพื่อลบจริง (หรือ `/co-many-clear confirm`)
- **ข้อมูลเกี่ยวกับปลั๊กอิน**: `/co-many about`

## การบันทึกโลก
- โลกที่ถูกสร้างโดยปลั๊กอินนี้จะถูกบันทึกในรูปแบบ `many_world/custom_world/world` ยกเว้นโลกเริ่มต้นของเซิร์ฟเวอร์
- ชื่อโลกใช้ได้เฉพาะ `a-z`, `0-9`, `_`, `-` (ตัวพิมพ์ใหญ่จะถูกแปลงเป็นตัวเล็ก)
- คำสั่ง delete / clear ใช้ได้กับโลกใน `many_world/` เท่านั้น จะไม่ลบโลกหลักของเซิร์ฟเวอร์
  ```text
  many_world/
  └─custom_world/
    └─world/
  ```
- หากสร้างโลกแบบ `-all` จะรวมทุกประเภทโลกไว้ในโฟลเดอร์เดียวกัน
  ```text
  many_world/
  └─custom_world/
     ├─world/
     ├─world_nether/
     └─world_the_end/
  ```
- **Paper 26.x** เก็บทุกโลกไว้ในโฟลเดอร์โลกหลักแทน: `world/dimensions/minecraft/many_world/custom_world/world`
  (ชื่อโลกในเกมยังเป็น `many_world/custom_world/world` เหมือนเดิม ดูตำแหน่งจริงได้ด้วย `/co-many about`)
- อัปเกรดจาก Paper 1.21 เป็น 26.x: โลกเดิมใน `many_world/` จะถูก Paper ย้ายไปไว้ใน `world/dimensions/...` ให้เองตอนเปิดเซิร์ฟเวอร์ครั้งแรก ปลั๊กอินหาเจอทั้งสองที่
  **สำรองโฟลเดอร์เซิร์ฟเวอร์ทั้งหมดก่อนอัปเกรดเสมอ**

## Permissions

| Permissions             | Command                                                                                         | Properties                                                |
|-------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| co.many.worlds.admin    | /co-many create <br/>/co-many tp <br/> /co-many list <br/> /co-many import <br/> /co-many about <br/> /co-many backup | สิทธิ์ในการจัดการโลก (สร้าง, วาร์ป, รายชื่อโลก, นำเข้าโลก, สำรองข้อมูลโลก) |
| co.many.worlds.admindel | /co-many delete <worldName>                                                                     | สิทธิ์ในการลบโลก                                          |
| co.many.worlds.adminclear | /co-many clear, /co-many-clear                                                                                | สิทธิ์ในการลบโลกถาวร                                       |

## การตั้งค่า

ไฟล์ `config.yml` จะถูกสร้างขึ้นเมื่อคุณเริ่มเซิร์ฟเวอร์ครั้งแรก คุณสามารถปรับการตั้งค่าได้ดังนี้:

```yaml
# Configuration file for CoManyWorld
default-world-type: -11  # Default to normal world
# Where players respawn when they die outside a CoManyWorld world (falls back to "world").
default-world: 'world'
# Used for portals when a CoManyWorld world has no Nether / End of its own
# (only when prevent-single-world-portals is false).
default-nether-world: 'world_nether'
default-end-world: 'world_the_end'
worlds: []   # List of worlds to be loaded on startup (managed by the plugin)
prevent-single-world-portals: true  # Prevent portal usage in worlds that don't have all 3 dimensions
```

ไฟล์ `delete_worlds.yml` มีไว้เก็บรายชื่อโลกที่ถูกนำออกจาก `config.yml` และรอการลบถาวร:
```yaml
delete_worlds: []
```

## การนำเข้าโลก

นำโฟลเดอร์โลกหรือไฟล์ ZIP ไปไว้ใน `plugins/CoManyWorld/import` แล้วใช้ `/co-many import <ชื่อไฟล์>` การคัดลอกทำในพื้นหลัง เซิร์ฟเวอร์จะไม่ค้าง
- ปลั๊กอินจะหาโฟลเดอร์ที่มี `level.dat` (โลก single player, Spigot, Paper 1.21) หรือโฟลเดอร์โลกของ Paper 26.x (มี `data/paper/level_overrides.dat`)
- โลกของ Paper 26.x (เช่นไฟล์ backup ที่ทำบน Paper 26.x) import ได้บน Paper 26.x เท่านั้น
- ไฟล์ `session.lock`, `uid.dat` และ `data/paper/metadata.dat` จะไม่ถูกคัดลอก (ไม่งั้นเซิร์ฟเวอร์จะมองว่าเป็นโลกซ้ำ)
- symbolic link จะไม่ถูกคัดลอก และถ้า ZIP แตกไฟล์แล้วใหญ่เกินพื้นที่ว่างในดิสก์ จะหยุดกลางทาง

## การสำรองข้อมูลโลก

คุณสามารถสำรองข้อมูลโลกได้ด้วยคำสั่ง `/co-many backup <worldName>` ปลั๊กอินจะ save โลกก่อน แล้วบีบอัดเป็น ZIP ในพื้นหลัง ไฟล์สำรองข้อมูลจะถูกเก็บไว้ใน `plugins/CoManyWorld/many_world_backup` (นำไฟล์นี้ไป import กลับได้)