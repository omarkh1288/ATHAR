Add-Type -AssemblyName System.Drawing

$src = "C:\Users\Omar\OneDrive\Desktop\android\assets\d2a86183f3650ad510e5554066e364f7473a20fa.png"
if (-Not (Test-Path $src)) {
    Write-Host "Source image not found in assets."
    exit 1
}

try {
    $img = [System.Drawing.Image]::FromFile($src)
    Write-Host "Image loaded: $($img.Width) x $($img.Height)"
    
    $destDrawable = "C:\Users\Omar\OneDrive\Desktop\android\app\src\main\res\drawable\athar_logo.png"
    $img.Save($destDrawable, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host "Saved to drawable/athar_logo.png"
    
    # Generate mipmap fallback icons
    $bases = @(
        @{ name = "mipmap-mdpi"; size = 48; roundSize = 48 },
        @{ name = "mipmap-hdpi"; size = 72; roundSize = 72 },
        @{ name = "mipmap-xhdpi"; size = 96; roundSize = 96 },
        @{ name = "mipmap-xxhdpi"; size = 144; roundSize = 144 },
        @{ name = "mipmap-xxxhdpi"; size = 192; roundSize = 192 }
    )
    
    # Safe zone for adaptive icons is usually 66% to 72% of the icon size to ensure it isn't masked.
    # We'll use 70% as the target size of the actual logo content inside the box.
    $safeFactor = 0.70
    
    foreach ($b in $bases) {
        $folder = "C:\Users\Omar\OneDrive\Desktop\android\app\src\main\res\$($b.name)"
        if (-Not (Test-Path $folder)) {
            New-Item -ItemType Directory -Force -Path $folder | Out-Null
        }
        
        # 1. Generate ic_launcher.png (Square / Legacy)
        $bmp = New-Object System.Drawing.Bitmap($b.size, $b.size)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        
        # Fill white background
        $g.Clear([System.Drawing.Color]::White)
        
        # Calculate aspect ratio
        $ratio = $img.Width / $img.Height
        $targetDim = $b.size * $safeFactor
        $newW = $targetDim
        $newH = $targetDim
        
        if ($ratio -gt 1) {
            $newH = $targetDim / $ratio
        } else {
            $newW = $targetDim * $ratio
        }
        $x = ($b.size - $newW) / 2
        $y = ($b.size - $newH) / 2
        
        $g.DrawImage($img, [float]$x, [float]$y, [float]$newW, [float]$newH)
        $bmp.Save("$folder\ic_launcher.png", [System.Drawing.Imaging.ImageFormat]::Png)
        $g.Dispose()
        $bmp.Dispose()
        
        # 2. Generate ic_launcher_round.png (Circular)
        $bmpRound = New-Object System.Drawing.Bitmap($b.roundSize, $b.roundSize)
        $gRound = [System.Drawing.Graphics]::FromImage($bmpRound)
        $gRound.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $gRound.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $gRound.Clear([System.Drawing.Color]::Transparent)
        
        # Draw a white circle
        $brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)
        $gRound.FillEllipse($brush, 0, 0, $b.roundSize, $b.roundSize)
        $brush.Dispose()
        
        # Draw the logo inside the circle
        $targetDimRound = $b.roundSize * $safeFactor
        $newW = $targetDimRound
        $newH = $targetDimRound
        if ($ratio -gt 1) {
            $newH = $targetDimRound / $ratio
        } else {
            $newW = $targetDimRound * $ratio
        }
        $x = ($b.roundSize - $newW) / 2
        $y = ($b.roundSize - $newH) / 2
        
        $gRound.DrawImage($img, [float]$x, [float]$y, [float]$newW, [float]$newH)
        $bmpRound.Save("$folder\ic_launcher_round.png", [System.Drawing.Imaging.ImageFormat]::Png)
        $gRound.Dispose()
        $bmpRound.Dispose()
        
        Write-Host "Saved $($b.name) icons."
    }
    
    $img.Dispose()
    Write-Host "SUCCESS!"
} catch {
    Write-Host "Error: $_"
}
