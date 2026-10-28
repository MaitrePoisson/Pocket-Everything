import shutil
import sys
from PIL import Image

src, = sys.argv[1:2]
shutil.copyfile(src, "docs/images/logo.png")
logo = Image.open(src).convert("RGBA")
logo.resize((512, 512), Image.LANCZOS).save("src/main/resources/logo.png", optimize=True)
logo.resize((128, 128), Image.LANCZOS).save("src/main/resources/pack.png", optimize=True)
