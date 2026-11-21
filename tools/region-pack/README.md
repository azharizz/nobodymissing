# Region pack builder (PRODUCT.md 6.13)

Builds `shared/regions/<id>.json` for one bounding box. The demo pack `aceh-demo.json` is **synthetic** (illustrative HAND values).

Real build (PENDING HUMAN: needs internet + GDAL):

```sh
# 1. Global HAND 30 m (AWS Open Data, glo-30-hand) clipped to the bbox
gdalwarp -te 97.99 4.22 98.03 4.26 -tr 0.001 0.001 /vsicurl/<HAND-30m-tile-url>.tif hand.tif
# 2. Optional: Copernicus DEM GLO-30, only if this region needs the higher-ground finder
gdalwarp -te 97.99 4.22 98.03 4.26 -tr 0.001 0.001 /vsicurl/<copernicus-glo30-tile-url>.tif dem.tif
# 3. Export the grid to the pack JSON format (grid.origin, grid.step, grid.rows/cols, grid.hand[row][col], row 0 = south)
gdal_translate -of AAIGrid hand.tif hand.asc
# 4. Shelters: OSM emergency=assembly_point / amenity=shelter (ODbL) + shelters typed by the organizer
```

Attribution to show in the app: "Produced using Copernicus WorldDEM-30 © DLR e.V." and "© OpenStreetMap contributors (ODbL)".
