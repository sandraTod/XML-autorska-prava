xquery version "3.1";
declare namespace k = "https://example.org/autorska-prava/korisnik";
declare variable $id as xs:string external;
let $collection := '/db/autorska-prava/korisnici'
return
    if (not(xmldb:collection-available($collection))) then error()
    else
        let $user := collection($collection)/k:korisnik[@id = $id]
        return if (count($user) gt 1) then error()
               else if (exists($user)) then $user else <missing/>
