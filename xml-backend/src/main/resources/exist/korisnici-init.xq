xquery version "3.1";
(: Pokrenuti jednom kao administrator, PRE uključivanja registracije.
   Ne brisati ili prepisivati lock dokument dok aplikacija radi. :)
let $parent := xmldb:create-collection('/db', 'autorska-prava')
let $collection := xmldb:create-collection($parent, 'korisnici')
return
    if (doc-available(concat($collection, '/registracija-lock.xml'))) then 'ALREADY_READY'
    else if (xmldb:store($collection, 'registracija-lock.xml', <lock/>, 'application/xml')) then 'READY'
    else error()
