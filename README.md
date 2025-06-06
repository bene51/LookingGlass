# Installation

- Make sure `Looking Glass Bridge` is installed and running (https://lookingglassfactory.com/software/looking-glass-bridge)

- Download the latest version of this plugin from the `Releases` section
  and copy it into the Fiji's  `plugins` folder.

- Make sure you have the newest version of 3Dscript installed, as described here:
  https://bene51.github.io/3Dscript/

- Restart Fiji.


# How to run it

- In Fiji, click on `Plugins > Looking Glass > Create and upload quilt`.

- The following dialog appears:

- Parameters:
  - `Animation file`: A 3D script for setting 3D properties. This can be used to
    contrast, opacity transfer function, channel colors... . **Important:** It does
    not need to contain the transformation for creating the quilt angles. The plugin
    takes care of this.
  
    The `Animation file` can be omitted, in which case the image will be rendered
    with default settings (the initial view when you open 3Dscript).

  - `Display`: Choose the Looking Glass display you own.

  - `View cone angle`: The angle enclosed by the left-most and right-most view.

  - `Focus`: Changes the focal plane of the display. There seems to be no information
    about the meaning of the numerical value to enter, you might need to experiment.
    -0.01 worked well for me, but maybe that was just my dataset.

  - `Resize input image`: Resizes the input image to roughly the size of a quilt tile,
    as needed by the choosen display. Needs extra time for resizing, but the remaining
    creation of the quilt is super-fast.
  
  - `Fit view to content`: Automatically center and zoom the 3D view to match the content.
    Basically, this renders a sample 3D view and defines content as pixels which more
    opaque than 30 (on a scale from 0 to 255).

  - `Hide scalebar`: Hide 3Dscript's scale bar.

  - `Hide border`: Hide 3Dscript's border.

- After clicking OK, the quilt image will be created, displayed and automatically
  uploaded to the display (if it is connected).
