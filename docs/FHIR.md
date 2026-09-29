# FHIR and the Canadian standards

Mediconnect exposes its data as **HL7 FHIR R4** and can export a patient's
record as a **pan-Canadian Patient Summary (PS-CA)**.

This document says exactly what that means, what has been verified, and what
has not — because "FHIR compliant" is a claim people make loosely, and the
difference between producing a conformant resource and being connected to a
provincial system is most of the work.

## Why it is here

The clinic's own API is shaped by the app's screens. It changes whenever a
screen changes, and nothing outside this repository could read it without
being told how. That is fine for one app talking to one server, and useless
the moment a patient wants their record to travel.

The FHIR layer is a second, read-only view of the same database, shaped by
the standard instead. Another Canadian system can read it without knowing
anything about Mediconnect.

## Architecture

```
          Android app                    Any FHIR client
               │                                │
               ▼                                ▼
     /api/mobile/…  (shaped by          /fhir/…  (shaped by
      the app's screens)                 HL7 FHIR R4 + CA profiles)
               │                                │
               └──────────┬─────────────────────┘
                          ▼
                   Spring services
                          │
                          ▼
                  JPA entities  →  Postgres / H2
```

Both views share the database and nothing else. The mapping between entities
and FHIR resources lives in one place, `FhirMapper`, so the REST endpoints
and the patient summary cannot drift apart.

Deliberately **not** the HAPI JPA server: that would bring its own
persistence layer and duplicate the schema this application already has.
Only HAPI's R4 model classes and its validator are used.

## What is served

`GET /fhir/metadata` returns the CapabilityStatement — what a FHIR client
asks for first, and the machine-readable version of this table.

| Resource | Profile claimed | Notes |
|---|---|---|
| `Patient` | CA Baseline `profile-patient` | Includes the jurisdictional health number |
| `Practitioner` | CA Baseline `profile-practitioner` | |
| `PractitionerRole` | CA Baseline `profile-practitionerrole` | Carries the specialty |
| `Organization` | CA Baseline `profile-organization` | The clinic |
| `Schedule` | *(none)* | See below |
| `Slot` | *(none)* | `?status=free` filters to bookable slots |
| `Appointment` | *(none)* | `?patient={id}` |
| `Patient/{id}/$summary` | PS-CA `bundle-ca-ps` | The patient summary document |

**Why scheduling claims no Canadian profile.** CA Baseline publishes no
`Appointment`, `Schedule` or `Slot` profile, and PS-CA is a patient-summary
guide with no scheduling content. These are therefore plain FHIR R4.
Stamping a Canadian profile URL on them would be inventing one, and a
profile URL that does not resolve is worse than none — it claims a
conformance nobody can check.

## The patient summary

`GET /fhir/Patient/{id}/$summary` returns a FHIR **document**: a Bundle whose
first entry is a Composition acting as the cover page, with every other
resource referenced from one of its sections and present in the same Bundle.
That self-containment is what lets another system open it.

PS-CA makes three sections mandatory:

| Section | LOINC | Built from |
|---|---|---|
| Medication Summary | `10160-0` | `MedicationStatement` |
| Allergies and Intolerances | `48765-2` | `AllergyIntolerance` |
| Problem List | `11450-4` | `Condition` |

They are present even when the patient has recorded nothing, carrying an
`emptyReason` of `unavailable` rather than entries. This is deliberate:
"no known allergies" and "nobody asked" are clinically different, and the
app has never asked a patient to confirm they have none, so claiming
`nilknown` would overstate what is known.

### Codes

Patients type free text — they do not know SNOMED CT. Entries are therefore
emitted as `CodeableConcept.text` with no coding until a clinician assigns
one. That is valid FHIR and it is honest; a guessed clinical code would be
neither.

## Identifiers

A health card number is meaningless without the URI saying which province
issued it, and that URI differs per jurisdiction. They are configuration,
not code:

```yaml
mediconnect:
  fhir:
    base-url: http://localhost:8080/fhir
```

with per-province systems in `FhirProperties`. **The defaults follow the
Infoway naming pattern and should be confirmed against the
[Canadian URI Registry](https://accelero.infoway-inforoute.ca/en/tools/developer-tools/canadian-uri-registry)
before any real deployment** — they decide whether another system recognises
the patient or treats them as a stranger. The registry was unreachable when
this was written, which is exactly why the value is configurable rather than
hardcoded.

## Booking for someone else

When an appointment is booked for a dependant, FHIR expresses it through
`Appointment.participant`: the dependant is the subject (`SBJ`), and the
account holder appears separately as the person who arranged it. Because the
dependant has no record of their own, they are carried as a display-only
reference rather than a link to a `Patient` that does not exist.

## What has been verified

`./gradlew test` runs the HL7 instance validator against the output. This is
the evidence behind every claim above; it is not a README assertion.

| Check | Status |
|---|---|
| Resources are valid FHIR R4 | ✅ verified by `FhirInstanceValidator` |
| Claimed profile URLs resolve in the published packages | ✅ verified |
| `Patient` satisfies CA Baseline structurally | ✅ verified |
| Summary is a valid document Bundle with all three sections | ✅ verified |
| Summary is self-contained (no dangling references) | ✅ verified |
| Terminology bindings (SNOMED CT, LOINC) | ⚠️ not checked — needs a terminology server |

CA Baseline 1.2.0 and PS-CA 2.1.1-DFT are vendored as published `.tgz`
packages under `src/test/resources/fhir-packages`, so conformance tests run
without network access in CI. A test that silently skips when a download
fails is worse than no test.

Two things worth knowing about those packages:

- The CA Baseline package is published as `1.2.0` but the profiles inside it
  are stamped `1.1.0`. The canonical URL is unversioned and is what the
  mapping claims, so this does not affect conformance.
- PS-CA's newer `ti-ballot` builds are produced by IG tooling that emits an
  `additional-binding` extension carrying both a value and nested
  extensions, which HAPI 7.4.5 refuses to parse. `2.1.1-DFT` is the newest
  build the validator on the classpath can read. The profile URLs are
  canonical and identical across both.

## What this is not

- **Not certified, and not connected.** Real exchange with a provincial
  system additionally needs SMART on FHIR, provincial onboarding and
  conformance testing through Canada Health Infoway. None of that lives
  here.
- **Not a write API.** Inbound FHIR is not accepted. Deciding whether an
  incoming `Patient` is somebody already on file is an identity-matching
  problem, not a parsing one, and doing it badly merges two people's
  records.
- **Not terminology-validated.** See the table above.

## References

- [CA Core+ (Canada Health Infoway)](https://infoscribe.infoway-inforoute.ca/spaces/PCI/pages/237240617/Pan-Canadian+Core+FHIR+Profile+Set+CA+Core)
- [CA Baseline (HL7 Canada)](https://build.fhir.org/ig/HL7-Canada/ca-baseline/)
- [PS-CA package](https://packages.simplifier.net/ca.infoway.io.psca)
- [HL7 FHIR R4](https://hl7.org/fhir/R4/)
