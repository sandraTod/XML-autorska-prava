xquery version "3.1";
declare namespace k = "https://example.org/autorska-prava/korisnik";
declare variable $email as xs:string external;
declare variable $xml as xs:string external;
let $collection := '/db/autorska-prava/korisnici'
let $lock := concat($collection, '/registracija-lock.xml')
return
    if (not(doc-available($lock))) then error()
    else util:exclusive-lock(doc($lock),
        if (exists(collection($collection)/k:korisnik[k:email = $email])) then <duplicate/>
        else
            let $user := parse-xml($xml)/k:korisnik
            let $filename := concat(util:hash($email, 'SHA-256'), '.xml')
            return if ($user/k:email != $email or empty($user)) then error()
                   else if (xmldb:store($collection, $filename, $user, 'application/xml')) then <created/>
                   else error()
    )
