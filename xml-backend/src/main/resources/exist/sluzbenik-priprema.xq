xquery version "3.1";
declare namespace k = "https://example.org/autorska-prava/korisnik";

let $source := doc('/db/autorska-prava/priprema/sluzbenik-primer.xml')/k:korisnik
let $collection := '/db/autorska-prava/korisnici'
let $lock := concat($collection, '/registracija-lock.xml')
let $email := lower-case(normalize-space(string($source/k:email)))
return
    if (count($source) != 1 or $source/k:uloga != 'SLUZBENIK'
        or $email != 'sluzbenik.razvoj@example.org' or string($source/k:email) != $email
        or not(doc-available($lock))) then error(xs:QName('INVALID_PREPARATION'), 'Proverite razvojni XML i inicijalizaciju korisnika.')
    else util:exclusive-lock(doc($lock),
        let $existing := collection($collection)/k:korisnik[lower-case(normalize-space(k:email)) = $email]
        let $filename := concat(util:hash($email, 'SHA-256'), '.xml')
        return
            if (count($existing) = 1 and $existing/k:uloga = 'SLUZBENIK') then <alreadyPresent/>
            else if (exists($existing)) then error(xs:QName('CONFLICT_EMAIL'), 'E-mail je zauzet; postojeći nalog nije promenjen.')
            else if (exists(collection($collection)/k:korisnik[@id = $source/@id])
                     or doc-available(concat($collection, '/', $filename))) then
                error(xs:QName('CONFLICT_ACCOUNT'), 'ID ili dokument je zauzet; nema prepisivanja.')
            else if (xmldb:store($collection, $filename, $source, 'application/xml')) then <created/>
            else error(xs:QName('STORE_FAILED'), 'Upis nije uspeo.')
    )
