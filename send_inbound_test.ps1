param(
    [string]$Type = "freeform_text"
)

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }

$phone = "918450932085" # Sahil Samel
$msgId = "wamid.test." + [System.Guid]::NewGuid().ToString().Substring(0, 8)
$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds().ToString()

$messageObj = @{
    from = $phone
    id = $msgId
    timestamp = $timestamp
}

switch ($Type) {
    "freeform_text" {
        $messageObj["type"] = "text"
        $messageObj["text"] = @{ body = "Hi, what are your gym hours and membership fees?" }
    }
    "button_reception" {
        $messageObj["type"] = "button"
        $messageObj["button"] = @{ text = "Chat with Reception"; payload = "CHAT_WITH_RECEPTION" }
    }
    "button_stop" {
        $messageObj["type"] = "button"
        $messageObj["button"] = @{ text = "Stop Notifications"; payload = "STOP" }
    }
    "media_image" {
        $messageObj["type"] = "image"
        $messageObj["image"] = @{
            id = "media_img_12345"
            caption = "Here is my payment screenshot"
            mime_type = "image/jpeg"
        }
        # Ensure mock image exists in storage for local dev testing
        $storageDir = Join-Path $PSScriptRoot "Launcher\storage\media\inch_pinch"
        if (!(Test-Path $storageDir)) { New-Item -ItemType Directory -Path $storageDir -Force | Out-Null }
        $mockImgFile = Join-Path $storageDir "media_img_12345.png"
        if (!(Test-Path $mockImgFile)) {
            $base64 = "iVBORw0KGgoAAAANSUhEUgAAASwAAAEsAQMAAABDsxw2AAAABlBMVEX///8AAABVwtN+AAAACXBIWXMAAA7EAAAOxAGVKw4bAAABTUlEQVRoge3ZyxGDMAwEUFFBSqBUWnUJVIBi9LNJOOTIZlbjA+DHSZZiE5HfYtOMXeSl9mzRZqOikT2D7ZG1zvqDLvtDWQ/Dkm+RgTHPu6f+lDGV2W9kwMxTL6tlX8j+gUVRt2UMMkw2WnTgMXvfyckezjQj2SXvHmRQ7DuS3UyRQbCt8j5fNNsh1yDDZOcmyo+xft7Rw66jlsmA2HnMqd1UHGPNZNJDkgExsVmJM2xcXxbAQobFNk935r1wRP4Ekz2C7VGqYWrfG6Wq3oTJsJjO0it0rQ7sC4AMkoX0s0x15niFDIx9tmXvzJdbMiQ2hz+zivap7NVChsS2yu8uo5zn4SuEDIhN3/Dl8r+MjC8PQgbGKu+Z/Zv+TIbKLGIprPpRzmSQLLv02BvL+DRBBsQ8PPVxk7P1ChkS04zKey9kPcYOWe93XGSPZb/FG3CAS8yR1EOAAAAAAElFTkSuQmCC"
            [IO.File]::WriteAllBytes($mockImgFile, [Convert]::FromBase64String($base64))
        }
    }
    "media_document" {
        $messageObj["type"] = "document"
        $messageObj["document"] = @{
            id = "media_doc_67890"
            caption = "Medical certificate"
            filename = "fitness_certificate.pdf"
            mime_type = "application/pdf"
        }
        # Ensure mock PDF exists in storage for local dev testing
        $storageDir = Join-Path $PSScriptRoot "Launcher\storage\media\inch_pinch"
        if (!(Test-Path $storageDir)) { New-Item -ItemType Directory -Path $storageDir -Force | Out-Null }
        $mockPdfFile = Join-Path $storageDir "media_doc_67890.pdf"
        if (!(Test-Path $mockPdfFile)) {
            $pdfContent = "%PDF-1.4`n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj`n2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj`n3 0 obj<</Type/Page/MediaBox[0 0 612 792]/Parent 2 0 R/Resources<<>>>>endobj`nxref`n0 4`n0000000000 65535 f `n0000000009 00000 n `n0000000052 00000 n `n0000000101 00000 n `ntrailer<</Size 4/Root 1 0 R>>`nstartxref`n178`n%%EOF"
            [IO.File]::WriteAllText($mockPdfFile, $pdfContent)
        }
    }
    "consent_stop" {
        $messageObj["type"] = "text"
        $messageObj["text"] = @{ body = "STOP" }
    }
    "consent_start" {
        $messageObj["type"] = "text"
        $messageObj["text"] = @{ body = "START" }
    }
    default {
        Write-Error "Unknown test type $Type"
        exit 1
    }
}

$payload = @{
    object = "whatsapp_business_account"
    entry = @(
        @{
            id = "WHATSAPP_BUSINESS_ACCOUNT_ID"
            changes = @(
                @{
                    field = "messages"
                    value = @{
                        messaging_product = "whatsapp"
                        metadata = @{
                            display_phone_number = "15551234567"
                            phone_number_id = "123456789"
                        }
                        messages = @($messageObj)
                    }
                }
            )
        }
    )
}

$jsonBody = $payload | ConvertTo-Json -Depth 10

Write-Host "`n>>> Dispatching Inbound Webhook: [$Type] (wamid: $msgId) ..." -ForegroundColor Cyan
$resp = Invoke-RestMethod -Uri "https://localhost:8080/alerts/webhook/whatsapp" -Method Post -Headers @{ "X-Tenant-ID" = "inch_pinch" } -ContentType "application/json" -Body $jsonBody
Write-Host "Webhook HTTP Response:" ($resp | ConvertTo-Json -Compress) -ForegroundColor Green
