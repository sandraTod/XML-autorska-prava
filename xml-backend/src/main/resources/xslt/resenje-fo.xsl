<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:r="https://example.org/autorska-prava/resenje"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="r">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <xsl:template match="/">
    <fo:root font-family="ResenjeFont" font-size="11pt" line-height="1.4" language="sr">
      <fo:layout-master-set>
        <fo:simple-page-master master-name="A4" page-width="210mm" page-height="297mm" margin="20mm">
          <fo:region-body margin-bottom="12mm"/>
          <fo:region-after extent="8mm"/>
        </fo:simple-page-master>
      </fo:layout-master-set>
      <fo:page-sequence master-reference="A4">
        <fo:static-content flow-name="xsl-region-after">
          <fo:block text-align="center" font-size="9pt">Strana <fo:page-number/></fo:block>
        </fo:static-content>
        <fo:flow flow-name="xsl-region-body">
          <fo:block font-size="20pt" space-after="8mm">Rešenje A-1 zahteva</fo:block>
          <fo:block space-after="3mm">ID rešenja:</fo:block>
          <fo:block font-size="9pt" wrap-option="wrap" space-after="5mm"><xsl:value-of select="r:resenje/@id"/></fo:block>
          <fo:block space-after="3mm">A-1 zahtev:</fo:block>
          <fo:block font-size="9pt" wrap-option="wrap" space-after="5mm"><xsl:value-of select="r:resenje/r:zahtevRef"/></fo:block>
          <fo:block space-after="5mm">Datum odluke: <xsl:value-of select="r:resenje/r:datumOdluke"/></fo:block>
          <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Službenik</fo:block>
          <fo:block>Ime: <xsl:value-of select="r:resenje/r:sluzbenik/r:ime"/></fo:block>
          <fo:block space-after="6mm">Prezime: <xsl:value-of select="r:resenje/r:sluzbenik/r:prezime"/></fo:block>
          <xsl:choose>
            <xsl:when test="r:resenje/r:odobrenje">
              <fo:block font-size="16pt" space-after="4mm" keep-with-next.within-page="always">Zahtev je odobren</fo:block>
              <fo:block>Šifra dela: <xsl:value-of select="r:resenje/r:odobrenje/r:sifraDela"/></fo:block>
            </xsl:when>
            <xsl:otherwise>
              <fo:block font-size="16pt" space-after="4mm" keep-with-next.within-page="always">Zahtev je odbijen</fo:block>
              <fo:block space-after="3mm" keep-with-next.within-page="always">Obrazloženje:</fo:block>
              <fo:block wrap-option="wrap" widows="2" orphans="2"><xsl:value-of select="r:resenje/r:odbijanje/r:obrazlozenje"/></fo:block>
            </xsl:otherwise>
          </xsl:choose>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>
</xsl:stylesheet>
