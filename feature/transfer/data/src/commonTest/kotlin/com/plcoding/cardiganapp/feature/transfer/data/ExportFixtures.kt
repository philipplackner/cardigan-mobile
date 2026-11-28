package com.plcoding.cardiganapp.feature.transfer.data

/** Shaped like cardigan-shared-specs `board-export-format` "Exact fields". */
fun exportJson(
    format: String = "\"cardigan.board\"",
    formatVersion: String = "1",
    key: String = "\"CRD\"",
): String =
    """
    {
      "format": $format,
      "formatVersion": $formatVersion,
      "exportedAt": "2026-11-04T23:30:00Z",
      "board": {
        "name": "Cardigan v1",
        "key": $key,
        "labels": [{ "name": "Bug", "color": "Red" }],
        "columns": [
          {
            "name": "To do",
            "cards": [
              {
                "number": 109,
                "title": "Fix login",
                "description": "",
                "labels": ["Bug"],
                "createdAt": "2026-11-01T10:00:00Z",
                "updatedAt": "2026-11-02T10:00:00Z"
              }
            ]
          }
        ]
      }
    }
    """.trimIndent()
