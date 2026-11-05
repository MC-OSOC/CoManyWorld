# co-many-world
![ข้อความอธิบายภาพ](backgrounds-01.png)

co-many-world เป็นปลั๊กอินสำหรับเซิร์ฟเวอร์ **Spigot และ Paper** Minecraft 1.21 ขึ้นไป (รวมถึง 26.x) ที่ช่วยให้คุณสามารถสร้างและจัดการหลายโลก
#### นี่คือปลั๊กอินตัวอย่างทดลองใช้งานและทดสอบเท่านั้น ไม่แนะนำให้ไปใช้กับ server จริง เพราะปลั๊กอินนี้อยู่ในช่วงพัฒนา และอาจจะไม่สมบูรณ์

[English version](https://github.com/MC-OSOC/co-many-world/blob/main/README-EN.md)
## คุณสมบัติ

- สร้างโลกใหม่ได้ง่ายดายด้วยคำสั่ง
- วาร์ปไปยังโลกต่าง ๆ ได้อย่างรวดเร็ว (ให้ผู้เล่นทั่วไปใช้ได้ และกำหนดสิทธิ์รายโลกได้)
- สร้างโลกได้หลายแบบ: ปกติ, Flat, Void (โลกว่าง), Large Biomes, Amplified หรือใช้ generator จากปลั๊กอินอื่น
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
`/co-many create <worldName> [-11|-12|-13|-all] [-s<seed>] [-flat|-void|-large|-amplified|-g<plugin[:id]>]`
- `-11` : โลกปกติ (ค่าเริ่มต้น)
- `-12` : โลกนรก (Nether)
- `-13` : โลก The End
- `-all`: สร้างโลกทั้งหมด
- `-s<seed>` : Seed

ชนิดของโลก (เลือกได้อย่างเดียว ใช้กับโลกปกติ `-11` หรือ `-all` เท่านั้น Nether / End ใน `-all` ยังสร้างแบบปกติ):
- `-flat` : โลกแบนราบ
- `-void` : โลกว่างเปล่า มีแท่นหิน 3x3 ให้ยืน (เหมาะกับ lobby / มินิเกม)
- `-large` : Large Biomes
- `-amplified` : Amplified
- `-g<plugin[:id]>` : ใช้ generator จากปลั๊กอินอื่น เช่น `-gMyGenerator` หรือ `-gMyGenerator:lobby` (ปลั๊กอินนั้นต้องติดตั้งและเปิดใช้งานอยู่)

ตัวอย่าง: `/co-many create lobby -11 -void` และ `/co-many create survival -all -s12345 -large`

ชนิดของโลกจะถูกบันทึกไว้ใน `config.yml` (หัวข้อ `world-settings`) เพื่อให้โหลดโลกแบบเดิมทุกครั้งที่เปิดเซิร์ฟเวอร์ และจะถูกลบออกเมื่อลบโลก

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

## Permissions

| Permissions             | Command                                                                                         | Properties                                                |
|-------------------------|-------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| co.many.worlds.admin    | /co-many create <br/>/co-many tp <br/> /co-many list <br/> /co-many import <br/> /co-many about <br/> /co-many backup | สิทธิ์ในการจัดการโลก (สร้าง, วาร์ป, รายชื่อโลก, นำเข้าโลก, สำรองข้อมูลโลก) รวมสิทธิ์ `co.many.worlds.tp` ด้วย |
| co.many.worlds.tp       | /co-many tp <worldName>                                                                         | สิทธิ์วาร์ปไปทุกโลก (ไม่ต้องเป็น admin) ค่าเริ่มต้น: op |
| co.many.worlds.tp.&lt;ชื่อโลก&gt; | /co-many tp &lt;ชื่อโลก&gt;                                                                  | สิทธิ์วาร์ปไปโลกนั้นโลกเดียว เช่น `co.many.worlds.tp.many_world/lobby/world` |
| co.many.worlds.admindel | /co-many delete <worldName>                                                                     | สิทธิ์ในการลบโลก                                          |
| co.many.worlds.adminclear | /co-many clear, /co-many-clear                                                                                | สิทธิ์ในการลบโลกถาวร                                       |

หากต้องการให้ผู้เล่นทั่วไปวาร์ปไปที่ lobby ได้ ให้เพิ่มสิทธิ์ `co.many.worlds.tp.many_world/lobby/world` (ผ่าน LuckPerms หรือปลั๊กอินสิทธิ์อื่น) ผู้เล่นที่ไม่มีสิทธิ์จะไม่เห็นโลกนั้นใน tab-complete และคำสั่งอื่น ๆ ของ `/co-many` ยังใช้ได้เฉพาะ admin

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
# world-settings:   # ปลั๊กอินเพิ่มให้เองเมื่อสร้างโลกด้วย -flat / -void / -large / -amplified / -g
#   many_world/lobby/world:
#     type: void
```

ไฟล์ `delete_worlds.yml` มีไว้เก็บรายชื่อโลกที่ถูกนำออกจาก `config.yml` และรอการลบถาวร:
```yaml
delete_worlds: []
```

## การนำเข้าโลก

นำโฟลเดอร์โลกหรือไฟล์ ZIP ไปไว้ใน `plugins/CoManyWorld/import` แล้วใช้ `/co-many import <ชื่อไฟล์>` ปลั๊กอินจะค้นหาไฟล์ `level.dat` เพื่อระบุตำแหน่งรากของโลก (ไฟล์ `session.lock` และ `uid.dat` จะไม่ถูกคัดลอก) การคัดลอกทำในพื้นหลัง เซิร์ฟเวอร์จะไม่ค้าง

## การสำรองข้อมูลโลก

คุณสามารถสำรองข้อมูลโลกได้ด้วยคำสั่ง `/co-many backup <worldName>` ปลั๊กอินจะ save โลกก่อน แล้วบีบอัดเป็น ZIP ในพื้นหลัง ไฟล์สำรองข้อมูลจะถูกเก็บไว้ใน `plugins/CoManyWorld/many_world_backup` (นำไฟล์นี้ไป import กลับได้)