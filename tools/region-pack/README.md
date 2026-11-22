# Region pack builder (PRODUCT.md 6.13)

Builds a region pack from real open data. No GDAL needed.

```sh
cd tools/region-pack && npm install
node build.mjs <id> <west> <south> <east> <north> <out.json>
# the demo pack: Kuala Simpang, Aceh Tamiang (flooded Nov 2025)
node build.mjs kuala-simpang 98.03 4.255 98.09 4.31 pack.json
```

- **HAND** (height above nearest drainage, m): Global HAND 30 m (`glo-30-hand`, AWS Open Data), read as a Cloud-Optimized GeoTIFF window, median per ~110 m cell.
- **Shelter candidates**: named schools, places of worship, hospitals and clinics from OpenStreetMap (Overpass). Flood-capable = HAND ≥ 5 m. The organizer confirms which are really open.
- `shared/demo/region-aceh.json` = this output + one SYNTHETIC demo shelter (SD3) and its in-shelter places. All demo people are synthetic.

Attribution shown in the app: "© OpenStreetMap contributors (ODbL)"; HAND derived from Copernicus GLO-30 DEM © DLR e.V. 2010-2014 and © Airbus 2014-2018.
