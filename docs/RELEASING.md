# การ build และออก Release ด้วย GitHub Actions

repo นี้มี workflow 2 ตัวใน `.github/workflows/`

| ไฟล์ | ทำงานเมื่อ | ทำอะไร |
|---|---|---|
| `ci.yml` | เปิด/อัปเดต Pull Request และ push เข้า `main` | รันเทสต์ (Paper API + MockBukkit) แล้ว build `.jar` (Spigot API) แนบไว้ในหน้า run ให้ดาวน์โหลดไปลองได้ 14 วัน |
| `release.yml` | push tag เช่น `1.1.0_Preview` **หรือ** กดปุ่ม *Run workflow* ในแท็บ Actions | ตรวจว่า tag ตรงกับเวอร์ชันใน `pom.xml` → รันเทสต์ → build `.jar` → สร้างไฟล์ `.sha256` → เซ็นรับรองที่มาของไฟล์ (attestation) → สร้าง Release พร้อมแนบไฟล์ |

ไฟล์ release ถูก build บนเครื่องของ GitHub จากโค้ดตรง tag นั้นเท่านั้น ไม่มีใครอัปโหลดไฟล์เองด้วยมือ

---

## ตั้งค่าครั้งแรก (ทำครั้งเดียว ต้องเป็น admin ของ repo)

### 1. เปิดใช้ GitHub Actions
**Settings → Actions → General**
- **Actions permissions:** เลือก *Allow all actions and reusable workflows*
  (หรือ *Allow actions created by GitHub* ก็พอ เพราะ workflow ใช้แค่ action ของ GitHub เอง)
- **Workflow permissions:** เลือก *Read repository contents permission* ไว้ได้เลย (ปลอดภัยกว่า)
  workflow ขอสิทธิ์เขียนเฉพาะงานสร้าง release เองในไฟล์ (`permissions:`)

> ถ้า repo อยู่ใน organization แล้วตั้งค่าตรงนี้ไม่ได้ ให้เจ้าของ organization เปิดที่
> **Organization settings → Actions → General**

### 2. เปิด Immutable releases (แนะนำ)
**Settings → General → Releases → Enable release immutability**

พอ publish แล้ว จะแก้ไฟล์แนบหรือย้าย tag ของ release นั้นไม่ได้อีก กันไม่ให้ใครแอบเปลี่ยนไฟล์ `.jar` ทีหลัง

### 3. ล็อก tag ด้วย Ruleset (แนะนำ)
**Settings → Rules → Rulesets → New ruleset → New tag ruleset**
- **Ruleset name:** `release tags`
- **Enforcement status:** Active
- **Target tags → Add target → Include all tags**
- **Bypass list:** เพิ่ม *Repository admin* (หรือทีมที่ดูแล release)
- ติ๊ก **Restrict creations**, **Restrict updates**, **Restrict deletions**
- กด **Create**

จากนี้มีแค่คนใน bypass list ที่สร้าง/ลบ tag ได้ ซึ่งก็คือคนที่ออก release ได้

> ถ้าจะออก release ด้วยปุ่ม *Run workflow* ด้วย ให้เพิ่ม **GitHub Actions** (หรือ *github-actions* app) ใน Bypass list
> เพราะตอนกดปุ่ม workflow เป็นคนสร้าง tag เอง ส่วนการกดปุ่ม Run workflow ทำได้เฉพาะคนที่มีสิทธิ์ write ใน repo

### 4. ป้องกัน branch `main` ให้ต้องผ่าน CI ก่อน merge (แนะนำ)
**Settings → Rules → Rulesets → New ruleset → New branch ruleset**
- **Target branches → Include default branch**
- ติ๊ก **Require a pull request before merging**
- ติ๊ก **Require status checks to pass** → **Add checks** → เลือก `build`
  (ชื่อนี้จะเลือกได้หลังจาก CI รันอย่างน้อยหนึ่งครั้ง)
- ติ๊ก **Block force pushes**

### 5. Dependabot
`.github/dependabot.yml` จะเปิด PR ให้เองทุกเดือนเมื่อ action ที่ใช้มีเวอร์ชันใหม่
(workflow ระบุ action ด้วย commit SHA เพื่อความปลอดภัย จึงต้องมีตัวช่วยอัปเดต)
ปกติเปิดใช้อัตโนมัติ ถ้าไม่เห็น PR ให้ดูที่ **Settings → Advanced Security → Dependabot version updates**

---

## วิธีออก Release

1. เปลี่ยนเวอร์ชันใน `pom.xml` เช่น `<version>1.2.0-Preview</version>`
2. เขียน release notes ไว้ที่ `.github/release-notes/<tag>.md` เช่น `.github/release-notes/1.2.0_Preview.md`
   (ถ้าไม่มีไฟล์นี้ GitHub จะสร้าง notes ให้อัตโนมัติจากรายชื่อ PR)
3. เปิด PR → รอ CI ผ่าน → merge เข้า `main`
4. **วิธีที่ 1: กดปุ่ม (ง่ายที่สุด)**
   **Actions → Release → Run workflow** → เลือก branch `main` → ใส่ tag เช่น `1.2.0_Preview` → กด **Run workflow**
   workflow จะรันเทสต์และ build ก่อน ถ้าผ่านทั้งหมดจึงสร้าง tag บน commit นั้นแล้วออก release

   **วิธีที่ 2: push tag เอง** สร้าง tag จาก `main` แล้ว push:
   ```bash
   git checkout main
   git pull
   git tag 1.2.0_Preview
   git push origin 1.2.0_Preview
   ```
   ชื่อ tag = เวอร์ชันใน `pom.xml` โดยเปลี่ยน `-` เป็น `_` (`1.2.0-Preview` → `1.2.0_Preview`)
   ถ้าไม่ตรงกัน workflow จะหยุดและไม่สร้าง release
5. ดูความคืบหน้าที่แท็บ **Actions → Release** ใช้เวลาไม่กี่นาที แล้ว Release จะขึ้นที่หน้า **Releases**
   - tag ที่มีคำว่า `Preview`, `alpha`, `beta` หรือ `-rc` จะถูกตั้งเป็น **pre-release** ให้อัตโนมัติ

> **ทางเลือก:** สร้าง Release ผ่านหน้าเว็บ (Releases → Draft a new release → สร้าง tag ใหม่) ก็ได้
> workflow จะแนบ `.jar` เข้า release นั้นให้เอง แต่ถ้าเปิด Immutable releases ไว้ ให้กด **Save draft**
> รอจนไฟล์ถูกแนบแล้วค่อยกด Publish

### ถ้า release ล้มเหลว
- ดูสาเหตุได้ในแท็บ **Actions** (log ของแต่ละขั้นตอน)
- แก้โค้ดผ่าน PR แล้วลบ tag ที่ล้มเหลวก่อนสร้างใหม่:
  ```bash
  git push origin :refs/tags/1.2.0_Preview
  git tag -d 1.2.0_Preview
  ```

---

## ผู้ใช้ตรวจสอบไฟล์ที่ดาวน์โหลดได้อย่างไร

**ตรวจว่าไฟล์มาจาก repo นี้จริง** (ต้องมี [GitHub CLI](https://cli.github.com/)):
```bash
gh attestation verify CoManyWorld-1.1.0-Preview.jar -R MC-OSOC/CoManyWorld
```
ถ้าผ่าน จะแสดงว่าไฟล์ถูก build โดย workflow `release.yml` จาก commit/tag ไหน

**ตรวจว่าไฟล์ไม่เสียระหว่างดาวน์โหลด:** ดาวน์โหลดไฟล์ `.sha256` คู่กันแล้วรัน
```bash
sha256sum -c CoManyWorld-1.1.0-Preview.jar.sha256
```

> Attestation ใช้ได้ฟรีกับ repo สาธารณะ ถ้า repo เป็น private ต้องใช้ GitHub Enterprise Cloud
> ถ้าไม่มี ขั้นตอน *Attest build provenance* จะล้ม ให้ลบขั้นตอนนั้นออกจาก `release.yml`
