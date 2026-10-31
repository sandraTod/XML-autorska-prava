export interface A1Pregled {
  documentId: string | null;
  naslov: string;
  vrsta: string;
}

export interface A1Adresa {
  ulica: string;
  broj: string;
  mesto: string;
  postanskiBroj?: string;
  drzava: string;
}

export interface A1FizickoLice {
  ime: string;
  prezime: string;
  adresa?: A1Adresa;
  drzavljanstvo: string;
}

export interface A1PravnoLice {
  poslovnoIme: string;
  sediste?: A1Adresa;
}

export interface A1Kontakt {
  telefon?: string;
  email?: string;
}

export interface A1Podnosilac {
  fizickoLice?: A1FizickoLice;
  pravnoLice?: A1PravnoLice;
  kontakt?: A1Kontakt;
}

export interface A1Punomocnik {
  ime: string;
  prezime: string;
  adresa?: A1Adresa;
}

export interface A1PreminuliAutor {
  ime: string;
  prezime: string;
  godinaSmrti: string;
}

export interface A1Autor {
  ziviAutor?: A1FizickoLice;
  preminuliAutor?: A1PreminuliAutor;
  pseudonimIliZnak?: string;
}

export interface A1Autorstvo {
  podnosilacJeAutor?: { pseudonimIliZnak?: string };
  anonimniAutor: boolean;
  poznatiAutori: A1Autor[];
}

export interface A1IzvornoDelo {
  naslov: string;
  autorstvo: A1Autorstvo;
}

export interface A1Delo {
  naslov: string;
  alternativniNaslov?: string;
  izvornoDelo?: A1IzvornoDelo;
  vrsta: string;
  formaZapisa: string;
  autorstvo: A1Autorstvo;
  stvorenoURadnomOdnosu?: boolean;
  nacinKoriscenja?: string;
}

export interface A1Potpis {
  imePotpisnika: string;
  prezimePotpisnika: string;
  referenca: string;
}

export interface A1Prilog {
  naziv: string;
  tip: string;
  referenca: string;
}

export interface A1SluzbeniDeo {
  brojPrijave: string;
  datumPodnosenja: string;
  primljeniPrilozi: A1Prilog[];
}

export interface A1Zahtev extends A1Pregled {
  id: string;
  podnosilac: A1Podnosilac;
  punomocnik?: A1Punomocnik;
  delo: A1Delo;
  potpis?: A1Potpis;
  prilozi: A1Prilog[];
  sluzbeniDeo?: A1SluzbeniDeo;
}

export interface A1Podnosenje {
  podnosilac: A1Podnosilac;
  punomocnik?: A1Punomocnik;
  delo: A1Delo;
  potpis: A1Potpis;
  prilozi: A1Prilog[];
}

export interface A1AutorUnos extends A1Autor {
  tip: 'ziviAutor' | 'preminuliAutor';
}
