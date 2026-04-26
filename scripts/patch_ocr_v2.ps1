$ErrorActionPreference = "Stop"
$file = "c:\MyProject\opencode-xiaoxiaojizhangben\TinyLedger_Project_20260406_211505\app\src\main\java\com\tinyledger\app\ui\screens\budget\ScreenshotAccountingScreen.kt"
$lines = [System.IO.File]::ReadAllLines($file)

$newLines = @()
$inPerformOcr = $false
$ocrBraceCount = 0
$skipUntilBraceZero = $false

for ($i = 0; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]

    # 1) After `var qianfanAvailable by remember`, add selectedEngine + minimaxAvailable
    if ($line -match 'var qianfanAvailable by remember') {
        $newLines += $line
        $newLines += ''
        $newLines += '    // ── OCR引擎选择（默认千帆OCR）'
        $newLines += '    var selectedEngine by remember { mutableStateOf(OcrEngineOption.QIANFAN) }'
        $newLines += '    var minimaxAvailable by remember { mutableStateOf(true) }'
        continue
    }

    # 2) Replace the entire `suspend fun performOcr(...)` function
    if ($line -match 'suspend fun performOcr\(bitmap') {
        $inPerformOcr = $true
        $ocrBraceCount = 0
        # Insert the new function
        $newLines += '    /**'
        $newLines += '     * 执行 OCR 识别（根据用户选择 + 优先级降级：千帆→MiniMax→Paddle→ML Kit）'
        $newLines += '     */'
        $newLines += '    suspend fun performOcr(bitmap: android.graphics.Bitmap): List<ParsedRecord> {'
        $newLines += '        val engine = selectedEngine'
        $newLines += '        Log.d("ScreenshotOCR", "用户选择引擎: ${engine.label}, 优先级=${engine.priority}")'
        $newLines += ''
        $newLines += '        // ── 按用户选择顺序 + 优先级降级尝试 ──'
        $newLines += '        val attemptOrder = buildList {'
        $newLines += '            add(engine)  // 用户选择优先'
        $newLines += '            // 补充剩余引擎，按优先级排序'
        $newLines += '            OcrEngineOption.entries'
        $newLines += '                .filter { it != engine }'
        $newLines += '                .sortedBy { it.priority }'
        $newLines += '                .forEach { add(it) }'
        $newLines += '        }'
        $newLines += ''
        $newLines += '        for (attempt in attemptOrder) {'
        $newLines += '            val result = when (attempt) {'
        $newLines += '                OcrEngineOption.QIANFAN -> {'
        $newLines += '                    Log.d("ScreenshotOCR", "  → 尝试千帆OCR...")'
        $newLines += '                    QianfanOcrEngine.recognize(bitmap, homeState.accounts).also {'
        $newLines += '                        if (it.isNotEmpty()) recognizedText = "千帆OCR: ${it.size} 条记录"'
        $newLines += '                        else Log.d("ScreenshotOCR", "  ⚠ 千帆OCR 无结果")'
        $newLines += '                    }'
        $newLines += '                }'
        $newLines += '                OcrEngineOption.MINIMAX -> {'
        $newLines += '                    Log.d("ScreenshotOCR", "  → 尝试MiniMax...")'
        $newLines += '                    MinimaxOcrEngine.recognize(bitmap, homeState.accounts).also {'
        $newLines += '                        if (it.isNotEmpty()) recognizedText = "MiniMax: ${it.size} 条记录"'
        $newLines += '                        else Log.d("ScreenshotOCR", "  ⚠ MiniMax 无结果")'
        $newLines += '                    }'
        $newLines += '                }'
        $newLines += '                OcrEngineOption.PADDLE -> {'
        $newLines += '                    if (!paddleAvailable) {'
        $newLines += '                        Log.d("ScreenshotOCR", "  ⚠ PaddleOCR 未就绪，跳过")'
        $newLines += '                        emptyList()'
        $newLines += '                    } else {'
        $newLines += '                        Log.d("ScreenshotOCR", "  → 尝试PaddleOCR...")'
        $newLines += '                        val processed = ImagePreprocessor.preprocess(bitmap)'
        $newLines += '                        val elements = PaddleOcrEngine.recognize(processed)'
        $newLines += '                        if (elements.isNotEmpty()) {'
        $newLines += '                            recognizedText = elements.joinToString(" ") { it.text }'
        $newLines += '                            ScreenshotTransactionParser.parseVisual(elements, homeState.accounts)'
        $newLines += '                        } else {'
        $newLines += '                            Log.d("ScreenshotOCR", "  ⚠ PaddleOCR 无结果")'
        $newLines += '                            emptyList()'
        $newLines += '                        }'
        $newLines += '                    }'
        $newLines += '                }'
        $newLines += '                OcrEngineOption.MLKIT -> {'
        $newLines += '                    Log.d("ScreenshotOCR", "  → 尝试ML Kit...")'
        $newLines += '                    val processed = ImagePreprocessor.preprocess(bitmap)'
        $newLines += '                    val image = com.google.mlkit.vision.common.InputImage.fromBitmap(processed, 0)'
        $newLines += '                    val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient('
        $newLines += '                        com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions.Builder().build()'
        $newLines += '                    )'
        $newLines += '                    val result = kotlinx.coroutines.suspendCancellableCoroutine<com.google.mlkit.vision.text.Text?> { cont ->'
        $newLines += '                        recognizer.process(image)'
        $newLines += '                            .addOnSuccessListener { cont.resume(it) }'
        $newLines += '                            .addOnFailureListener { cont.resume(null) }'
        $newLines += '                    }'
        $newLines += '                    if (result == null) {'
        $newLines += '                        recognizedText = "ML Kit 识别失败"'
        $newLines += '                        emptyList()'
        $newLines += '                    } else {'
        $newLines += '                        recognizedText = result.text'
        $newLines += '                        val ocrElements = extractOcrElements(result)'
        $newLines += '                        if (ocrElements.isNotEmpty()) {'
        $newLines += '                            ScreenshotTransactionParser.parseVisual(ocrElements, homeState.accounts)'
        $newLines += '                        } else {'
        $newLines += '                            ScreenshotTransactionParser.parse(result.text, homeState.accounts)'
        $newLines += '                        }'
        $newLines += '                    }'
        $newLines += '                }'
        $newLines += '            }'
        $newLines += '            if (result.isNotEmpty()) {'
        $newLines += '                Log.d("ScreenshotOCR", "\u2705 \u4F7F\u7528 ${attempt.label}: ${result.size} \u6761\u8BB0\u5F55")'
        $newLines += '                return result'
        $newLines += '            }'
        $newLines += '        }'
        $newLines += ''
        $newLines += '        Log.w("ScreenshotOCR", "\u274C \u6240\u6709OCR\u5F15\u64CE\u5747\u5931\u8D25")'
        $newLines += '        recognizedText = "\u6240\u6709OCR\u5F15\u64CE\u5747\u65E0\u7ED3\u679C"'
        $newLines += '        return emptyList()'
        $newLines += '    }'
        continue
    }

    # Skip old performOcr body lines
    if ($inPerformOcr) {
        # Count braces to find end of function
        $openBraces = ($line.ToCharArray() | Where-Object { $_ -eq '{' }).Count
        $closeBraces = ($line.ToCharArray() | Where-Object { $_ -eq '}' }).Count
        $ocrBraceCount += $openBraces - $closeBraces
        
        if ($ocrBraceCount -le 0 -and $line -match '^\s*\}\s*$') {
            $inPerformOcr = $false
            continue
        }
        if ($ocrBraceCount -lt 0) {
            $inPerformOcr = $false
            continue
        }
        continue
    }

    # 3) Add radio buttons row BEFORE the "Select image button" comment
    if ($line -match '// Select image button') {
        $newLines += ''
        $newLines += '        // ── OCR引擎选择 ──'
        $newLines += '        item {'
        $newLines += '            Card('
        $newLines += '                modifier = Modifier.fillMaxWidth(),'
        $newLines += '                shape = RoundedCornerShape(10.dp),'
        $newLines += '                colors = CardDefaults.cardColors('
        $newLines += '                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)'
        $newLines += '                )'
        $newLines += '            ) {'
        $newLines += '                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {'
        $newLines += '                    Text('
        $newLines += '                        "OCR\u8BC6\u522B\u65B9\u6848",'
        $newLines += '                        style = MaterialTheme.typography.labelSmall,'
        $newLines += '                        color = MaterialTheme.colorScheme.onSurfaceVariant,'
        $newLines += '                        modifier = Modifier.padding(bottom = 4.dp)'
        $newLines += '                    )'
        $newLines += '                    Row('
        $newLines += '                        modifier = Modifier.fillMaxWidth(),'
        $newLines += '                        horizontalArr = Arrangement.SpaceEvenly,'
        $newLines += '                        verticalAlignment = Alignment.CenterVertically'
        $newLines += '                    ) {'
        $newLines += '                        OcrEngineOption.entries.forEach { option ->'
        $newLines += '                            Row('
        $newLines += '                                modifier = Modifier'
        $newLines += '                                    .clip(RoundedCornerShape(6.dp))'
        $newLines += '                                    .clickable { selectedEngine = option }'
        $newLines += '                                    .padding(horizontal = 4.dp, vertical = 4.dp),'
        $newLines += '                                verticalAlignment = Alignment.CenterVertically'
        $newLines += '                            ) {'
        $newLines += '                                RadioButton('
        $newLines += '                                    selected = selectedEngine == option,'
        $newLines += '                                    onClick = { selectedEngine = option },'
        $newLines += '                                    modifier = Modifier.size(16.dp),'
        $newLines += '                                    colors = RadioButtonDefaults.colors('
        $newLines += '                                        selectedColor = MaterialTheme.colorScheme.primary'
        $newLines += '                                    )'
        $newLines += '                                )'
        $newLines += '                                Spacer(modifier = Modifier.width(2.dp))'
        $newLines += '                                Text('
        $newLines += '                                    text = option.label,'
        $newLines += '                                    style = MaterialTheme.typography.labelSmall.copy('
        $newLines += '                                        fontSize = 10.sp,'
        $newLines += '                                        fontWeight = if (selectedEngine == option) FontWeight.Bold else FontWeight.Normal,'
        $newLines += '                                        color = if (selectedEngine == option)'
        $newLines += '                                            MaterialTheme.colorScheme.primary'
        $newLines += '                                        else MaterialTheme.colorScheme.onSurfaceVariant'
        $newLines += '                                    )'
        $newLines += '                                )'
        $newLines += '                            }'
        $newLines += '                        }'
        $newLines += '                    }'
        $newLines += '                }'
        $newLines += '            }'
        $newLines += '        }'
        $newLines += ''
    }

    $newLines += $line
}

[System.IO.File]::WriteAllLines($file, $newLines)
Write-Host "All ScreenshotAccountingScreen patches applied successfully."
