🚀 **Project Spotlight: MeshPay – Offline UPI using Bluetooth Mesh**

I built **MeshPay**, a concept project that explores how digital payments can work even **without internet connectivity**.

---

🔴 **The Problem**
UPI payments fail in low-network areas (villages, basements, crowded events).
No internet = No payment ❌

---

🟢 **The Idea / Solution**
MeshPay uses a **Bluetooth-based mesh network** to send payments offline.

* User creates a payment
* It gets **encrypted** 🔐
* Nearby phones pass it along (like a relay)
* A device with internet (bridge) uploads it
* Server verifies & completes the transaction

---

⚙️ **Key Features**

* 🔐 **Secure Encryption (RSA + AES)**
* 🔁 **Idempotency** (no duplicate payments)
* 📡 **Store & Forward (Mesh Gossip)**
* 🌐 **Automatic Online/Offline switching**

---

👍 **Why it’s useful**

* Works in **no network zones**
* Helpful for **rural areas & disaster situations**
* Shows how payments can be **offline-first**

---

⚠️ **Limitations (Real Talk)**

* Payment is **not instant** (settles later)
* Possible **double-spend attempts** before settlement
* Needs **wallet-based system** (not real bank integration yet)

---

💡 **Final Thought**
This project doesn’t replace UPI — it **extends it** for offline scenarios using **mesh networking + cryptography**.

---


