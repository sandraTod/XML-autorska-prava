<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:a1="https://example.org/autorska-prava/a1"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="a1">
  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <xsl:template match="/">
    <html lang="sr-Latn" xml:lang="sr-Latn">
      <head>
        <title>A-1 zahtev</title>
        <style type="text/css">
          body { font-family: sans-serif; max-width: 850px; margin: 2em auto; padding: 0 1em; color: #243247; }
          h2 { border-top: 1px solid #dce2e9; padding-top: 1em; }
          p { overflow-wrap: anywhere; }
          .stavka { border-left: 3px solid #dce2e9; padding-left: 1em; margin: 1em 0; }
        </style>
      </head>
      <body>
        <h1>A-1 zahtev</h1>
        <xsl:if test="a1:zahtevA1/@id">
          <p>ID zahteva: <xsl:value-of select="a1:zahtevA1/@id"/></p>
        </xsl:if>
        <xsl:if test="a1:zahtevA1/@vlasnikId">
          <p>ID vlasnika: <xsl:value-of select="a1:zahtevA1/@vlasnikId"/></p>
        </xsl:if>
        <h2>Podnosilac</h2>
        <xsl:choose>
          <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice">
            <h3>Fizičko lice</h3>
            <p>Ime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:ime"/></p>
            <p>Prezime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:prezime"/></p>
            <h3>Adresa</h3>
            <p>Ulica: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:ulica"/></p>
            <p>Broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:broj"/></p>
            <p>Mesto: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:mesto"/></p>
            <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:postanskiBroj">
              <p>Poštanski broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:postanskiBroj"/></p>
            </xsl:if>
            <p>Država: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:adresa/a1:drzava"/></p>
            <p>Državljanstvo: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:fizickoLice/a1:drzavljanstvo"/></p>
          </xsl:when>
          <xsl:otherwise>
            <h3>Pravno lice</h3>
            <p>Poslovno ime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:poslovnoIme"/></p>
            <h3>Sedište</h3>
            <p>Ulica: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:ulica"/></p>
            <p>Broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:broj"/></p>
            <p>Mesto: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:mesto"/></p>
            <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:postanskiBroj">
              <p>Poštanski broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:postanskiBroj"/></p>
            </xsl:if>
            <p>Država: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:pravnoLice/a1:sediste/a1:drzava"/></p>
          </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:telefon or a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:email">
          <h3>Kontakt</h3>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:telefon">
            <p>Telefon: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:telefon"/></p>
          </xsl:if>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:email">
            <p>Email: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:podnosilac/a1:kontakt/a1:email"/></p>
          </xsl:if>
        </xsl:if>
        <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik">
          <h2>Punomoćnik</h2>
          <p>Ime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:ime"/></p>
          <p>Prezime: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:prezime"/></p>
          <h3>Adresa</h3>
          <p>Ulica: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:ulica"/></p>
          <p>Broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:broj"/></p>
          <p>Mesto: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:mesto"/></p>
          <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:postanskiBroj">
            <p>Poštanski broj: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:postanskiBroj"/></p>
          </xsl:if>
          <p>Država: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:punomocnik/a1:adresa/a1:drzava"/></p>
        </xsl:if>
        <h2>Delo</h2>
        <p>Naslov: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:naslov"/></p>
        <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:alternativniNaslov">
          <p>Alternativni naslov: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:alternativniNaslov"/></p>
        </xsl:if>
        <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo">
          <div class="stavka">
            <h3>Izvorno delo</h3>
            <p>Naslov: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:naslov"/></p>
            <h3>Autorstvo</h3>
            <xsl:choose>
              <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:podnosilacJeAutor">
                <p>Podnosilac je autor.</p>
                <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:podnosilacJeAutor/a1:pseudonimIliZnak">
                  <p>Pseudonim ili znak: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:podnosilacJeAutor/a1:pseudonimIliZnak"/></p>
                </xsl:if>
              </xsl:when>
              <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:poznatiAutori">
                <xsl:for-each select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:izvornoDelo/a1:autorstvo/a1:poznatiAutori/a1:autor">
                  <div class="stavka">
                    <h4>Autor <xsl:value-of select="position()"/></h4>
                    <xsl:choose>
                      <xsl:when test="a1:ziviAutor">
                        <p>Živi autor</p>
                        <p>Ime: <xsl:value-of select="a1:ziviAutor/a1:ime"/></p>
                        <p>Prezime: <xsl:value-of select="a1:ziviAutor/a1:prezime"/></p>
                        <h3>Adresa</h3>
                        <p>Ulica: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:ulica"/></p>
                        <p>Broj: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:broj"/></p>
                        <p>Mesto: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:mesto"/></p>
                        <xsl:if test="a1:ziviAutor/a1:adresa/a1:postanskiBroj">
                          <p>Poštanski broj: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:postanskiBroj"/></p>
                        </xsl:if>
                        <p>Država: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:drzava"/></p>
                        <p>Državljanstvo: <xsl:value-of select="a1:ziviAutor/a1:drzavljanstvo"/></p>
                      </xsl:when>
                      <xsl:otherwise>
                        <p>Preminuli autor</p>
                        <p>Ime: <xsl:value-of select="a1:preminuliAutor/a1:ime"/></p>
                        <p>Prezime: <xsl:value-of select="a1:preminuliAutor/a1:prezime"/></p>
                        <p>Godina smrti: <xsl:value-of select="a1:preminuliAutor/a1:godinaSmrti"/></p>
                      </xsl:otherwise>
                    </xsl:choose>
                    <xsl:if test="a1:pseudonimIliZnak">
                      <p>Pseudonim ili znak: <xsl:value-of select="a1:pseudonimIliZnak"/></p>
                    </xsl:if>
                  </div>
                </xsl:for-each>
              </xsl:when>
              <xsl:otherwise><p>Anonimni autor.</p></xsl:otherwise>
            </xsl:choose>
          </div>
        </xsl:if>
        <p>Vrsta: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:vrsta"/></p>
        <p>Forma zapisa: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:formaZapisa"/></p>
        <h3>Autorstvo</h3>
        <xsl:choose>
          <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:podnosilacJeAutor">
            <p>Podnosilac je autor.</p>
            <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:podnosilacJeAutor/a1:pseudonimIliZnak">
              <p>Pseudonim ili znak: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:podnosilacJeAutor/a1:pseudonimIliZnak"/></p>
            </xsl:if>
          </xsl:when>
          <xsl:when test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:poznatiAutori">
            <xsl:for-each select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:autorstvo/a1:poznatiAutori/a1:autor">
              <div class="stavka">
                <h4>Autor <xsl:value-of select="position()"/></h4>
                <xsl:choose>
                  <xsl:when test="a1:ziviAutor">
                    <p>Živi autor</p>
                    <p>Ime: <xsl:value-of select="a1:ziviAutor/a1:ime"/></p>
                    <p>Prezime: <xsl:value-of select="a1:ziviAutor/a1:prezime"/></p>
                    <h3>Adresa</h3>
                    <p>Ulica: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:ulica"/></p>
                    <p>Broj: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:broj"/></p>
                    <p>Mesto: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:mesto"/></p>
                    <xsl:if test="a1:ziviAutor/a1:adresa/a1:postanskiBroj">
                      <p>Poštanski broj: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:postanskiBroj"/></p>
                    </xsl:if>
                    <p>Država: <xsl:value-of select="a1:ziviAutor/a1:adresa/a1:drzava"/></p>
                    <p>Državljanstvo: <xsl:value-of select="a1:ziviAutor/a1:drzavljanstvo"/></p>
                  </xsl:when>
                  <xsl:otherwise>
                    <p>Preminuli autor</p>
                    <p>Ime: <xsl:value-of select="a1:preminuliAutor/a1:ime"/></p>
                    <p>Prezime: <xsl:value-of select="a1:preminuliAutor/a1:prezime"/></p>
                    <p>Godina smrti: <xsl:value-of select="a1:preminuliAutor/a1:godinaSmrti"/></p>
                  </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="a1:pseudonimIliZnak">
                  <p>Pseudonim ili znak: <xsl:value-of select="a1:pseudonimIliZnak"/></p>
                </xsl:if>
              </div>
            </xsl:for-each>
          </xsl:when>
          <xsl:otherwise><p>Anonimni autor.</p></xsl:otherwise>
        </xsl:choose>
        <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:stvorenoURadnomOdnosu">
          <p>Stvoreno u radnom odnosu:
            <xsl:choose>
              <xsl:when test="normalize-space(a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:stvorenoURadnomOdnosu) = 'true' or normalize-space(a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:stvorenoURadnomOdnosu) = '1'">Da</xsl:when>
              <xsl:otherwise>Ne</xsl:otherwise>
            </xsl:choose>
          </p>
        </xsl:if>
        <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:nacinKoriscenja">
          <p>Način korišćenja: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:delo/a1:nacinKoriscenja"/></p>
        </xsl:if>
        <h2>Potpis</h2>
        <p>Ime potpisnika: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:potpis/a1:imePotpisnika"/></p>
        <p>Prezime potpisnika: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:potpis/a1:prezimePotpisnika"/></p>
        <p>Referenca: <xsl:value-of select="a1:zahtevA1/a1:podaciGradjanina/a1:potpis/a1:referenca"/></p>
        <xsl:if test="a1:zahtevA1/a1:podaciGradjanina/a1:prilozi/a1:prilog">
          <h2>Prilozi</h2>
          <xsl:for-each select="a1:zahtevA1/a1:podaciGradjanina/a1:prilozi/a1:prilog">
            <div class="stavka">
              <h4>Prilog <xsl:value-of select="position()"/></h4>
              <p>Naziv: <xsl:value-of select="a1:naziv"/></p>
              <p>Tip: <xsl:value-of select="a1:tip"/></p>
              <p>Referenca: <xsl:value-of select="a1:referenca"/></p>
            </div>
          </xsl:for-each>
        </xsl:if>
        <xsl:if test="a1:zahtevA1/a1:sluzbeniDeo">
          <h2>Službeni deo</h2>
          <p>Broj prijave: <xsl:value-of select="a1:zahtevA1/a1:sluzbeniDeo/a1:brojPrijave"/></p>
          <p>Datum podnošenja: <xsl:value-of select="a1:zahtevA1/a1:sluzbeniDeo/a1:datumPodnosenja"/></p>
          <xsl:if test="a1:zahtevA1/a1:sluzbeniDeo/a1:primljeniPrilozi/a1:prilog">
            <h3>Primljeni prilozi</h3>
            <xsl:for-each select="a1:zahtevA1/a1:sluzbeniDeo/a1:primljeniPrilozi/a1:prilog">
              <div class="stavka">
                <h4>Prilog <xsl:value-of select="position()"/></h4>
                <p>Naziv: <xsl:value-of select="a1:naziv"/></p>
                <p>Tip: <xsl:value-of select="a1:tip"/></p>
                <p>Referenca: <xsl:value-of select="a1:referenca"/></p>
              </div>
            </xsl:for-each>
          </xsl:if>
        </xsl:if>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
