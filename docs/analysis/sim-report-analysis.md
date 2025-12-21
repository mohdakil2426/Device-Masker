# SIM Card Spoofing Consistency Report  
## Ensuring Realistic and Low-Risk Telephony Identity Spoofing

---

## 1. Introduction

SIM card spoofing in Android systems involves overriding telephony-related identifiers such as IMSI, ICCID, phone number (MSISDN), MCC/MNC, and carrier metadata.  
Effective spoofing **does not rely on copying or randomizing values**, but on maintaining **logical consistency across all SIM-related parameters**.

This document explains:
- Which SIM values **must be synchronized**
- Which values **must remain unique**
- Why **consistency matters more than identical data**

---

## 2. Core Principle

> **SIM spoofing must be logically consistent, not identical.**

Applications and system integrity checks do not verify whether values are “real,” but whether they **make sense together** as a valid carrier-issued SIM profile.

---

## 3. Classification of SIM Values

SIM-related values can be divided into **three categories** based on how strictly they must align.

---

## 4. Category 1: Mandatory Synchronized Values (Hard Consistency)

These values **must belong to the same country and the same carrier**.  
Any mismatch here significantly increases detection risk.

| Parameter | Requirement |
|--------|------------|
| Phone Number (MSISDN) | Country code and carrier-compatible |
| MCC (Mobile Country Code) | Must match the selected country |
| MNC (Mobile Network Code) | Must match the selected carrier |
| IMSI (Prefix) | Must start with MCC + MNC |
| ICCID (Country Prefix) | Must match SIM country |
| Carrier Name | Must correspond to MNC |
| SIM Country ISO | Must match country (e.g., `IN`) |

**Example (India – Airtel):**
```

Phone Number: +91XXXXXXXXXX
MCC: 404
MNC: 10
IMSI: 40410XXXXXXXXX
ICCID: 899110XXXXXXXXX
Carrier Name: Airtel
SIM Country ISO: IN

```

---

## 5. Category 2: Unique but Valid Values (Controlled Variability)

These values **must be different**, as they represent unique SIM identities, but still follow valid carrier patterns.

| Parameter | Rule |
|--------|----|
| IMSI Subscriber Digits | Must vary per SIM |
| ICCID Serial Portion | Must be unique |
| Phone Number | Must be unique |
| SIM Slot Index | Device-dependent |

⚠️ Using identical values across profiles may indicate cloning behavior and raise suspicion.

---

## 6. Category 3: Soft Consistency Values (Contextual Alignment)

These values are not always directly validated but are commonly cross-checked for plausibility.

| Parameter | Example (India) |
|--------|---------------|
| Network Country ISO | `in` |
| Time Zone | `Asia/Kolkata` |
| Locale | `en-IN`, `hi-IN` |
| Roaming Status | Typically `false` |

Inconsistent soft values can silently reduce trust scores.

---

## 7. Correct vs Incorrect Configuration Examples

### ❌ Incorrect (Inconsistent)
```

Phone Number: +91XXXXXXXXXX
MCC: 404
MNC: 10
Carrier Name: Vodafone UK
IMSI: 23415XXXXXXXXX

```
**Issue:** Country and carrier mismatch

---

### ✅ Correct (Consistent)
```

Phone Number: +91XXXXXXXXXX
MCC: 404
MNC: 10
Carrier Name: Airtel
IMSI: 40410XXXXXXXXX
ICCID: 899110XXXXXXXXX

```
**Result:** Logically valid SIM profile

---

## 8. Key Insight

Modern applications and system checks evaluate:
- Whether the SIM identity **could exist**
- Whether all values **agree with each other**

They do **not** require:
- Identical values
- Real subscriber data
- Perfect carrier databases

---

## 9. Final Conclusion

> **SIM spoofing success depends on relationship correctness, not data sameness.**

- Same country → **Required**
- Same carrier → **Required**
- Unique subscriber identifiers → **Required**
- Random mismatches → **High detection risk**

---

## 10. Best Practice Recommendation

- Use **carrier-based presets** (e.g., India – Airtel, India – Jio)
- Generate values using **pattern-based logic**
- Avoid random or mixed-country data
- Maintain internal consistency across all telephony APIs

---

