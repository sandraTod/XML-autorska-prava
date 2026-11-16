<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:a1="https://example.org/autorska-prava/a1"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="a1">
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
          <fo:block font-size="20pt" space-after="8mm">A-1 zahtev</fo:block>
          <xsl:if test="a1:zahtevA1/@id">
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2" font-size="9pt">ID zahteva: <xsl:value-of select="a1:zahtevA1/@id"/></fo:block>
          </xsl:if>
          <xsl:if test="a1:zahtevA1/@vlasnikId">
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2" font-size="9pt">ID vlasnika: <xsl:value-of select="a1:zahtevA1/@vlasnikId"/></fo:block>
          </xsl:if>
          <fo:block font-size="16pt" space-before="6mm" space-after="4mm" keep-with-next.within-page="always">Podnosilac</fo:block>
          <xsl:choose>
            <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice">
              <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Fizičko lice</fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:ime"/></fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Prezime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:prezime"/></fo:block>
              <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Adresa</fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ulica: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:ulica"/></fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:broj"/></fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Mesto: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:mesto"/></fo:block>
              <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:postanskiBroj">
                <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Poštanski broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:postanskiBroj"/></fo:block>
              </xsl:if>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Država: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:drzava"/></fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Državljanstvo: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:drzavljanstvo"/></fo:block>
            </xsl:when>
            <xsl:otherwise>
              <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Pravno lice</fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Poslovno ime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:poslovnoIme"/></fo:block>
              <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Sedište</fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ulica: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:ulica"/></fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:broj"/></fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Mesto: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:mesto"/></fo:block>
              <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:postanskiBroj">
                <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Poštanski broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:postanskiBroj"/></fo:block>
              </xsl:if>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Država: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:drzava"/></fo:block>
            </xsl:otherwise>
          </xsl:choose>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:telefon or a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:email">
            <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Kontakt</fo:block>
            <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:telefon">
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Telefon: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:telefon"/></fo:block>
            </xsl:if>
            <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:email">
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Email: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:email"/></fo:block>
            </xsl:if>
          </xsl:if>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik">
            <fo:block font-size="16pt" space-before="6mm" space-after="4mm" keep-with-next.within-page="always">Punomoćnik</fo:block>
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:ime"/></fo:block>
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Prezime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:prezime"/></fo:block>
            <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Adresa</fo:block>
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ulica: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:ulica"/></fo:block>
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:broj"/></fo:block>
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Mesto: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:mesto"/></fo:block>
            <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:postanskiBroj">
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Poštanski broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:postanskiBroj"/></fo:block>
            </xsl:if>
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Država: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:drzava"/></fo:block>
          </xsl:if>
          <fo:block font-size="16pt" space-before="6mm" space-after="4mm" keep-with-next.within-page="always">Delo</fo:block>
          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Naslov: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:naslov"/></fo:block>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:alternativniNaslov">
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Alternativni naslov: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:alternativniNaslov"/></fo:block>
          </xsl:if>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo">
            <fo:block border-left="0.5pt solid #dce2e9" padding-left="3mm" space-before="3mm" space-after="3mm">
              <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Izvorno delo</fo:block>
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Naslov: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:naslov"/></fo:block>
              <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Autorstvo</fo:block>
              <xsl:choose>
                <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:podnosilacJeAutor">
                  <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Podnosilac je autor.</fo:block>
                  <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:podnosilacJeAutor/a1:pseudonimIliZnak">
                    <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Pseudonim ili znak: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:podnosilacJeAutor/a1:pseudonimIliZnak"/></fo:block>
                  </xsl:if>
                </xsl:when>
                <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:poznatiAutori">
                  <xsl:for-each select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:poznatiAutori/a1:autor">
                    <fo:block border-left="0.5pt solid #dce2e9" padding-left="3mm" space-before="3mm" space-after="3mm">
                      <fo:block font-size="12pt" space-before="3mm" space-after="2mm" keep-with-next.within-page="always">Autor <xsl:value-of select="position()"/></fo:block>
                      <xsl:choose>
                        <xsl:when test="a1:ziviAutor">
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Živi autor</fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ime: <xsl:value-of select="a1:ziviAutor/a1:ime"/></fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Prezime: <xsl:value-of select="a1:ziviAutor/a1:prezime"/></fo:block>
                          <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Adresa</fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ulica: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:ulica"/></fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Broj: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:broj"/></fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Mesto: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:mesto"/></fo:block>
                          <xsl:if test="a1:ziviAutor/a1:adresa/a1:postanskiBroj">
                            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Poštanski broj: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:postanskiBroj"/></fo:block>
                          </xsl:if>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Država: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:drzava"/></fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Državljanstvo: <xsl:value-of select="a1:ziviAutor/a1:drzavljanstvo"/></fo:block>
                        </xsl:when>
                        <xsl:otherwise>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Preminuli autor</fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ime: <xsl:value-of select="a1:preminuliAutor/a1:ime"/></fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Prezime: <xsl:value-of select="a1:preminuliAutor/a1:prezime"/></fo:block>
                          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Godina smrti: <xsl:value-of select="a1:preminuliAutor/a1:godinaSmrti"/></fo:block>
                        </xsl:otherwise>
                      </xsl:choose>
                      <xsl:if test="a1:pseudonimIliZnak">
                        <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Pseudonim ili znak: <xsl:value-of select="a1:pseudonimIliZnak"/></fo:block>
                      </xsl:if>
                    </fo:block>
                  </xsl:for-each>
                </xsl:when>
                <xsl:otherwise><fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Anonimni autor.</fo:block></xsl:otherwise>
              </xsl:choose>
            </fo:block>
          </xsl:if>
          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Vrsta: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:vrsta"/></fo:block>
          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Forma zapisa: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:formaZapisa"/></fo:block>
          <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Autorstvo</fo:block>
          <xsl:choose>
            <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:podnosilacJeAutor">
              <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Podnosilac je autor.</fo:block>
              <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:podnosilacJeAutor/a1:pseudonimIliZnak">
                <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Pseudonim ili znak: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:podnosilacJeAutor/a1:pseudonimIliZnak"/></fo:block>
              </xsl:if>
            </xsl:when>
            <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:poznatiAutori">
              <xsl:for-each select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:poznatiAutori/a1:autor">
                <fo:block border-left="0.5pt solid #dce2e9" padding-left="3mm" space-before="3mm" space-after="3mm">
                  <fo:block font-size="12pt" space-before="3mm" space-after="2mm" keep-with-next.within-page="always">Autor <xsl:value-of select="position()"/></fo:block>
                  <xsl:choose>
                    <xsl:when test="a1:ziviAutor">
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Živi autor</fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ime: <xsl:value-of select="a1:ziviAutor/a1:ime"/></fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Prezime: <xsl:value-of select="a1:ziviAutor/a1:prezime"/></fo:block>
                      <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Adresa</fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ulica: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:ulica"/></fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Broj: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:broj"/></fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Mesto: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:mesto"/></fo:block>
                      <xsl:if test="a1:ziviAutor/a1:adresa/a1:postanskiBroj">
                        <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Poštanski broj: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:postanskiBroj"/></fo:block>
                      </xsl:if>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Država: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:drzava"/></fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Državljanstvo: <xsl:value-of select="a1:ziviAutor/a1:drzavljanstvo"/></fo:block>
                    </xsl:when>
                    <xsl:otherwise>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Preminuli autor</fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ime: <xsl:value-of select="a1:preminuliAutor/a1:ime"/></fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Prezime: <xsl:value-of select="a1:preminuliAutor/a1:prezime"/></fo:block>
                      <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Godina smrti: <xsl:value-of select="a1:preminuliAutor/a1:godinaSmrti"/></fo:block>
                    </xsl:otherwise>
                  </xsl:choose>
                  <xsl:if test="a1:pseudonimIliZnak">
                    <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Pseudonim ili znak: <xsl:value-of select="a1:pseudonimIliZnak"/></fo:block>
                  </xsl:if>
                </fo:block>
              </xsl:for-each>
            </xsl:when>
            <xsl:otherwise><fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Anonimni autor.</fo:block></xsl:otherwise>
          </xsl:choose>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:stvorenoURadnomOdnosu">
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Stvoreno u radnom odnosu:
              <xsl:choose>
                <xsl:when test="normalize-space(a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:stvorenoURadnomOdnosu) = 'true' or normalize-space(a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:stvorenoURadnomOdnosu) = '1'">Da</xsl:when>
                <xsl:otherwise>Ne</xsl:otherwise>
              </xsl:choose>
            </fo:block>
          </xsl:if>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:nacinKoriscenja">
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Način korišćenja: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:nacinKoriscenja"/></fo:block>
          </xsl:if>
          <fo:block font-size="16pt" space-before="6mm" space-after="4mm" keep-with-next.within-page="always">Potpis</fo:block>
          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Ime potpisnika: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:potpis/a1:imePotpisnika"/></fo:block>
          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Prezime potpisnika: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:potpis/a1:prezimePotpisnika"/></fo:block>
          <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Referenca: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:potpis/a1:referenca"/></fo:block>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:prilozi/a1:prilog">
            <fo:block font-size="16pt" space-before="6mm" space-after="4mm" keep-with-next.within-page="always">Prilozi</fo:block>
            <xsl:for-each select="a1:zahtevA1/a1:podaciGradjanina/a1:prilozi/a1:prilog">
              <fo:block border-left="0.5pt solid #dce2e9" padding-left="3mm" space-before="3mm" space-after="3mm">
                <fo:block font-size="12pt" space-before="3mm" space-after="2mm" keep-with-next.within-page="always">Prilog <xsl:value-of select="position()"/></fo:block>
                <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Naziv: <xsl:value-of select="a1:naziv"/></fo:block>
                <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Tip: <xsl:value-of select="a1:tip"/></fo:block>
                <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Referenca: <xsl:value-of select="a1:referenca"/></fo:block>
              </fo:block>
            </xsl:for-each>
          </xsl:if>
          <xsl:if test="a1:zahtevA1/a1:sluzbeniDeo">
            <fo:block font-size="16pt" space-before="6mm" space-after="4mm" keep-with-next.within-page="always">Službeni deo</fo:block>
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Broj prijave: <xsl:value-of select="a1:zahtevA1/a1:sluzbeniDeo/a1:brojPrijave"/></fo:block>
            <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Datum podnošenja: <xsl:value-of select="a1:zahtevA1/a1:sluzbeniDeo/a1:datumPodnosenja"/></fo:block>
            <xsl:if test="a1:zahtevA1/a1:sluzbeniDeo/a1:primljeniPrilozi/a1:prilog">
              <fo:block font-size="14pt" space-before="4mm" space-after="3mm" keep-with-next.within-page="always">Primljeni prilozi</fo:block>
              <xsl:for-each select="a1:zahtevA1/a1:sluzbeniDeo/a1:primljeniPrilozi/a1:prilog">
                <fo:block border-left="0.5pt solid #dce2e9" padding-left="3mm" space-before="3mm" space-after="3mm">
                  <fo:block font-size="12pt" space-before="3mm" space-after="2mm" keep-with-next.within-page="always">Prilog <xsl:value-of select="position()"/></fo:block>
                  <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Naziv: <xsl:value-of select="a1:naziv"/></fo:block>
                  <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Tip: <xsl:value-of select="a1:tip"/></fo:block>
                  <fo:block space-after="2mm" wrap-option="wrap" widows="2" orphans="2">Referenca: <xsl:value-of select="a1:referenca"/></fo:block>
                </fo:block>
              </xsl:for-each>
            </xsl:if>
          </xsl:if>
        </fo:flow>
      </fo:page-sequence>
    </fo:root>
  </xsl:template>
</xsl:stylesheet>
