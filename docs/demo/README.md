# Demo video — Sagay Tourist Navigator

A screen recording of the native Android app running on a physical device
(mirrored inside Android Studio's **Running Devices** panel), captured while
planning a trip from **Old Sagay** into **Sagay City**.

![Sagay Tourist Navigator app demo](sagay-tourist-navigator-demo.mp4)

## What the recording shows

1. **Destination search** — the searchable sheet over the map, listing tourist
   spots and schools with their assigned tricycle colour.
2. **The Old Sagay to Sagay City journey** — a two-leg trip shown as
   **Yellow tricycle** to the change point, then **Red tricycle** for the final
   leg, with the change point named
   (*Sagay City Bus Terminal (Poblacion)*).
3. **The corridor fare** — one fare for the whole journey (**₱20**, or **₱16**
   with the 20% senior/student/PWD discount applied), taken from the official
   fare matrix of Resolution No. 2026-117 rather than a distance formula.
4. **The mapped route** — the OSRM road geometry between origin and
   destination.

## Video details

| Property | Value |
| --- | --- |
| File | `sagay-tourist-navigator-demo.mp4` |
| Duration | 2 min 55 s |
| Resolution | 1280 × 720 |
| Frame rate | 30 fps |
| Size | 7.65 MB |
| Codec | H.264 (yuv420p) + AAC |

The original capture was 125.6 MB, which is above GitHub's 100 MB limit for
files stored in a repository, so it was re-encoded with ffmpeg at CRF 20
(`-preset medium`, AAC 64 kbps, `+faststart`). Picture quality was kept as
close to the source as possible while bringing the file well under the limit;
the audio track was untouched in length.

Source recording: `2026-11-18 13-42-27.mp4` (kept out of the repository
because of the 100 MB limit).

---

See the [app documentation](../APP_DOCUMENTATION.md) for the user guide,
architecture and fare rules, and the
[native app README](../../SagayTouristNavigation/README.md) for the data
replacement guide.
