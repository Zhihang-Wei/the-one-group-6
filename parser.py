import json
from PIL import Image, ImageDraw, ImageFont
from shapely import wkt

im = Image.new('RGBA', (500, 500), (128, 128, 128,255))
draw = ImageDraw.Draw(im)
fontsize = 32
fnt = ImageFont.truetype("/usr/share/fonts/gnu-free/FreeMono.otf", fontsize)

with open("fmi2.json","r") as f:
    tmp = f.read()
    data = json.loads(tmp)
    print(data)

    for hub in data:
        print(hub)

        geom = wkt.loads(hub["polygon"])

        if geom.geom_type != "Polygon":
            print(f"ERROR: Type_ID == {geom.geo_type}")
            continue

        draw.text((geom.centroid.x-((len(hub["name"])/2)*(fontsize*1.125/2)), geom.centroid.y-(fontsize*1.25/2)), hub["name"], font=fnt, fill="red")

        x,y = geom.exterior.xy
        assert len(x) == len(y)

        poly = []
        for i in range(len(x)):
            poly.append((x[i],y[i]))

        draw.polygon(poly, outline="black")

im.save('data/background.png', quality=95)
