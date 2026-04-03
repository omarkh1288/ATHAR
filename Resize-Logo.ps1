Add-Type -AssemblyName System.Drawing

$src = "C:\Users\Omar\OneDrive\Desktop\android\LOGO"
if (-Not (Test-Path $src)) {
    Write-Host "LOGO file not found."
    exit 1
}

try {
    $img = [System.Drawing.Image]::FromFile($src)
    Write-Host "Image loaded: $($img.Width) x $($img.Height)"
    
    $destDrawable = "C:\Users\Omar\OneDrive\Desktop\android\app\src\main\res\drawable\athar_logo.png"
    $img.Save($destDrawable, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host "Saved to drawable/athar_logo.png"
    
    # Also generate mipmap fallback icons
    $bases = @(
        @{ name = "mipmap-mdpi"; size = 48 },
        @{ name = "mipmap-hdpi"; size = 72 },
        @{ name = "mipmap-xhdpi"; size = 96 },
        @{ name = "mipmap-xxhdpi"; size = 144 },
        @{ name = "mipmap-xxxhdpi"; size = 192 }
    )
    
    foreach ($b in $bases) {
        $folder = "C:\Users\Omar\OneDrive\Desktop\android\app\src\main\res\$($b.name)"
        if (-Not (Test-Path $folder)) {
            New-Item -ItemType Directory -Force -Path $folder | Out-Null
        }
        
        $bmp = New-Object System.Drawing.Bitmap($b.size, $b.size)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        
        # Calculate aspect ratio
        $ratio = $img.Width / $img.Height
        $newW = $b.size
        $newH = $b.size
        
        # We'll just draw it centered and scaled to fit the box
        if ($ratio -gt 1) {
            $newH = $b.size / $ratio
        } else {
            $newW = $b.size * $ratio
        }
        $x = ($b.size - $newW) / 2
        $y = ($b.size - $newH) / 2
        
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.DrawImage($img, [float]$x, [float]$y, [float]$newW, [float]$newH)
        
        # save as ic_launcher.png and ic_launcher_round.png
        $bmp.Save("$folder\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Save("$folder\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)
        
        Write-Host "Saved $($b.name) icons."
        
        $g.Dispose()
        $bmp.Dispose()
    }
    
    $img.Dispose()
} catch {
    Write-Host "Error: $_"
}
