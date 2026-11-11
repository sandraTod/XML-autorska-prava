<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:r="https://example.org/autorska-prava/resenje"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="r">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <xsl:template match="/">
    <html lang="sr-Latn" xml:lang="sr-Latn">
      <head>
        <title>Rešenje A-1 zahteva</title>
        <style type="text/css">
          body { font-family: sans-serif; max-width: 850px; margin: 2em auto; padding: 0 1em; color: #243247; }
          h2 { border-top: 1px solid #dce2e9; padding-top: 1em; }
          p { overflow-wrap: anywhere; white-space: pre-wrap; }
        </style>
      </head>
      <body>
        <h1>Rešenje A-1 zahteva</h1>
        <p>ID rešenja: <xsl:value-of select="r:resenje/@id"/></p>
        <p>A-1 zahtev: <xsl:value-of select="r:resenje/r:zahtevRef"/></p>
        <p>Datum odluke: <xsl:value-of select="r:resenje/r:datumOdluke"/></p>
        <h2>Službenik</h2>
        <p>Ime: <xsl:value-of select="r:resenje/r:sluzbenik/r:ime"/></p>
        <p>Prezime: <xsl:value-of select="r:resenje/r:sluzbenik/r:prezime"/></p>
        <xsl:choose>
          <xsl:when test="r:resenje/r:odobrenje">
            <h2>Zahtev je odobren</h2>
            <p>Šifra dela: <xsl:value-of select="r:resenje/r:odobrenje/r:sifraDela"/></p>
          </xsl:when>
          <xsl:otherwise>
            <h2>Zahtev je odbijen</h2>
            <p>Obrazloženje: <xsl:value-of select="r:resenje/r:odbijanje/r:obrazlozenje"/></p>
          </xsl:otherwise>
        </xsl:choose>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
