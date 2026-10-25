# Robust Zero-Dependency HTTP & Reverse Proxy Server for Freelance Frontend UI
param(
    [int]$Port = 3000,
    [string]$BackendBase = "https://localhost:8080"
)

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
[System.Net.ServicePointManager]::ServerCertificateValidationCallback = { $true }

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://localhost:$Port/")
$listener.Prefixes.Add("http://127.0.0.1:$Port/")
$listener.Start()

$uiDir = $PSScriptRoot
Write-Host "================================================================================" -ForegroundColor Green
Write-Host "[UI Server] Freelance Frontend UI running at: http://localhost:$Port/" -ForegroundColor Green
Write-Host "[UI Server] Reverse proxying API requests to: $BackendBase" -ForegroundColor Cyan
Write-Host "[UI Server] Serving static files from: $uiDir" -ForegroundColor Cyan
Write-Host "================================================================================" -ForegroundColor Green

$mimeTypes = @{
    ".html" = "text/html; charset=utf-8"
    ".css"  = "text/css; charset=utf-8"
    ".js"   = "application/javascript; charset=utf-8"
    ".json" = "application/json; charset=utf-8"
    ".png"  = "image/png"
    ".jpg"  = "image/jpeg"
    ".svg"  = "image/svg+xml"
    ".ico"  = "image/x-icon"
}

try {
    while ($listener.IsListening) {
        try {
            $context = $listener.GetContext()
            $request = $context.Request
            $response = $context.Response

            $rawUrl = $request.Url.LocalPath
            if ($rawUrl -eq "/" -or [string]::IsNullOrWhiteSpace($rawUrl)) {
                $rawUrl = "/index.html"
            }

            # Add CORS & No-Cache Headers to all responses
            $response.AddHeader("Access-Control-Allow-Origin", "*")
            $response.AddHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH")
            $response.AddHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Tenant-ID, X-Trace-ID, Accept")
            $response.AddHeader("Cache-Control", "no-cache, no-store, must-revalidate")
            $response.AddHeader("Pragma", "no-cache")
            $response.AddHeader("Expires", "0")

            # Handle CORS Preflight OPTIONS
            if ($request.HttpMethod -eq "OPTIONS") {
                $response.StatusCode = 200
                $response.OutputStream.Close()
                continue
            }

            $cleanPath = $rawUrl.TrimStart("/").Replace("/", [System.IO.Path]::DirectorySeparatorChar)
            $filePath = Join-Path $uiDir $cleanPath

            # 1. If physical static file exists in ui/, serve it
            if ((Test-Path $filePath -PathType Leaf) -and ![string]::IsNullOrWhiteSpace([System.IO.Path]::GetExtension($filePath))) {
                $ext = [System.IO.Path]::GetExtension($filePath).ToLower()
                $mime = if ($mimeTypes.ContainsKey($ext)) { $mimeTypes[$ext] } else { "application/octet-stream" }
                $response.ContentType = $mime
                
                $bytes = [System.IO.File]::ReadAllBytes($filePath)
                $response.ContentLength64 = $bytes.Length
                $response.OutputStream.Write($bytes, 0, $bytes.Length)
                $response.OutputStream.Close()
                continue
            }

            # 2. If it's an API route (e.g. /auth, /configurations, /members, /plan, etc.), proxy to Backend
            $isApiRoute = $rawUrl.StartsWith("/auth") -or `
                          $rawUrl.StartsWith("/support") -or `
                          $rawUrl.StartsWith("/configurations") -or `
                          $rawUrl.StartsWith("/members") -or `
                          $rawUrl.StartsWith("/plan") -or `
                          $rawUrl.StartsWith("/tenant") -or `
                          $rawUrl.StartsWith("/user") -or `
                          $rawUrl.StartsWith("/alerts") -or `
                          $rawUrl.StartsWith("/roles") -or `
                          $rawUrl.StartsWith("/authorizations") -or `
                          $rawUrl.StartsWith("/audit")

            if ($isApiRoute) {
                try {
                    $targetUrl = "$BackendBase$($request.Url.PathAndQuery)"
                    $proxyReq = [System.Net.HttpWebRequest]::Create($targetUrl)
                    $proxyReq.Method = $request.HttpMethod
                    $proxyReq.ServerCertificateValidationCallback = { $true }

                    # Copy Request Headers
                    foreach ($headerKey in $request.Headers.AllKeys) {
                        if ($headerKey -eq "Content-Type") {
                            $proxyReq.ContentType = $request.ContentType
                        } elseif ($headerKey -eq "Accept") {
                            $proxyReq.Accept = $request.Headers["Accept"]
                        } elseif ($headerKey -eq "User-Agent") {
                            $proxyReq.UserAgent = $request.UserAgent
                        } elseif ($headerKey -eq "Content-Length" -or $headerKey -eq "Host" -or $headerKey -eq "Connection") {
                            # Skip restricted headers
                        } else {
                            try {
                                $proxyReq.Headers.Add($headerKey, $request.Headers[$headerKey])
                            } catch {}
                        }
                    }

                    # Copy Request Body if present
                    if ($request.HasEntityBody) {
                        $reqStream = $request.InputStream
                        $proxyReqStream = $proxyReq.GetRequestStream()
                        $reqStream.CopyTo($proxyReqStream)
                        $reqStream.Close()
                        $proxyReqStream.Close()
                    }

                    # Execute Backend Request
                    try {
                        $proxyResp = $proxyReq.GetResponse()
                    } catch [System.Net.WebException] {
                        $proxyResp = $_.Exception.Response
                    }

                    if ($proxyResp -ne $null) {
                        $httpResp = [System.Net.HttpWebResponse]$proxyResp
                        $response.StatusCode = [int]$httpResp.StatusCode
                        $response.ContentType = $httpResp.ContentType

                        $respStream = $httpResp.GetResponseStream()
                        $respStream.CopyTo($response.OutputStream)
                        $respStream.Close()
                        $httpResp.Close()
                    } else {
                        $response.StatusCode = 502
                        $errBytes = [System.Text.Encoding]::UTF8.GetBytes('{"error":"Bad Gateway - Backend Unreachable"}')
                        $response.ContentType = "application/json"
                        $response.OutputStream.Write($errBytes, 0, $errBytes.Length)
                    }
                } catch {
                    $response.StatusCode = 500
                    $errBytes = [System.Text.Encoding]::UTF8.GetBytes("{""error"":""Proxy Error: $($_.Exception.Message)""}")
                    $response.ContentType = "application/json"
                    $response.OutputStream.Write($errBytes, 0, $errBytes.Length)
                }

                $response.OutputStream.Close()
                continue
            }

            # 3. Otherwise, fallback to index.html for SPA router
            $indexPath = Join-Path $uiDir "index.html"
            $bytes = [System.IO.File]::ReadAllBytes($indexPath)
            $response.ContentType = "text/html; charset=utf-8"
            $response.ContentLength64 = $bytes.Length
            $response.OutputStream.Write($bytes, 0, $bytes.Length)
            $response.OutputStream.Close()
        } catch {
            # Ignore individual client stream disconnects
        }
    }
} finally {
    $listener.Stop()
}
