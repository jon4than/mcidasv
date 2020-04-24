# These RGB functions were obtained from the JMA (Japan Meteorological
# Agency) RGB Training Library:
# https://www.jma.go.jp/jma/jma-eng/satellite/RGB_TL.html
# Note that the interpretation of the output of these functions is
# still ongoing and there is a possibility the functions may change.

# Some of these functions include a line containing the "resampleGrid"
# function.  This is done to resample the resolution of the red band
# to the highest resolution band being passed through the composite.
# The reason for this is that combineRGB returns a data object that
# is the resolution of the red band.  Doing this resampleGrid allows
# for a higher resolution display than would otherwise be available.

# AHI Natural Color RGB
def AHINaturalColorRGB(b3A, b4A, b5A):
    # red = band5; 0% to 100% rescalled to 0 to 255
    # grn = band4; 0% to 100% rescalled to 0 to 255
    # blu = band3; 0% to 100% rescalled to 0 to 255
    hr_b5A = resampleGrid(b5A, b3A)
    red = rescale(hr_b5A, 0, 100, 0, 255)
    grn = rescale(b4A, 0, 100, 0, 255)
    blu = rescale(b3A, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)

# AHI Night Microphysics RGB
def AHINightMicrophysicsRGB(b7T, b13T, b15T):
    # red = band15 - band13; -4K to 2K rescaled to 0 to 255
    # grn = band13 - band7; 0K to 10K rescaled to 0 to 255
    # blu = band13; 243K to 293K rescaled to 0 to 255
    red = rescale(b15T-b13T, -4, 2, 0, 255)
    grn = rescale(b13T-b7T, 0, 10, 0, 255)
    blu = rescale(b13T, 243, 293, 0, 255)
    return combineRGB(red, grn, blu)

# AHI Day Convective Storm RGB
def AHIDayConvectiveStormRGB(b3A, b5A, b7T, b8T, b10T, b13T):
    # red = band8 - band10; -35K to 5K rescaled to 0 to 255
    # grn = band7 - band13; -5K to 60K rescaled to 0 to 255; gamma 0.5
    # blu = band5 - band3; -75% to 25%% rescaled to 0 to 255
    hr_b8T = resampleGrid(b8T, b3A)
    hr_b10T = resampleGrid(b10T, b3A)
    red = rescale(hr_b8T-hr_b10T, -35, 5, 0, 255)
    grn = 255*(rescale(b7T-b13T, -5, 60, 0, 1)**2)
    blu = rescale(b5A-b3A, -75, 25, 0, 255)
    return combineRGB(red, grn, blu)

# AHI Airmass RGB
def AHIAirmassRGB(b8T, b10T, b12T, b13T):
    # red = band8 - band10; -25K to 0K rescaled to 0 to 255
    # grn = band12 - band13; -40K to 5K rescaled to 0 to 255
    # blu = band8; 243K to 208K rescaled to 0 to 255
    red = rescale(b8T-b10T, -25, 0, 0, 255)
    grn = rescale(b12T-b13T, -40, 5, 0, 255)
    blu = rescale(b8T, 243, 208, 0, 255)
    return combineRGB(red, grn, blu)

# AHI Ash RGB
def AHIAshRGB(b11T, b13T, b15T):
    # red = band15 - band13; -4K to 2K rescaled to 0 to 255
    # grn = band13 - band11; -4K to 5K rescaled to 0 to 255
    # blu = band13; 243K to 208K rescaled to 0 to 255
    red = rescale(b15T-b13T, -4, 2, 0, 255)
    grn = rescale(b13T-b11T, -4, 5, 0, 255)
    blu = rescale(b13T, 243, 208, 0, 255)
    return combineRGB(red, grn, blu)

# AHI True Color RGB
def AHITrueColorRGB(b1A, b2A, b3A):
    # red = band3; 0% to 100% rescaled to 0 to 255
    # grn = band2; 0% to 100% rescaled to 0 to 255
    # blu = band1; 0% to 100% rescaled to 0 to 255
    red = rescale(b3A, 0, 100, 0, 255)
    grn = rescale(b2A, 0, 100, 0, 255)
    blu = rescale(b1A, 0, 100, 0, 255)
    return combineRGB(red, grn, blu)
    
# AHI Dust RGB
def AHIDustRGB(b11T, b13T, b15T):
    # red = band15 - band13; -4K to 2K rescaled to 0 to 255
    # grn = band13 - band11; 0K to 15K rescaled to 0 to 255; gamma 2.5
    # blu = band13; 261K to 289K rescaled to 0 to 255
    red = rescale(b15T-b13T, -4, 2, 0, 255)
    grn = 255*(rescale(b13T-b11T, 0, 15, 0, 1)**0.4)
    blu = rescale(b13T, 261, 289, 0, 255)
    return combineRGB(red, grn, blu)
