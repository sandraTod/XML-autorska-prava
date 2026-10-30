xquery version "3.1";
declare variable $documentId as xs:string external;
declare variable $xml as xs:string external;

let $parent := '/db/autorska-prava'
let $requests := concat($parent, '/a1')
let $resolutions := concat($parent, '/resenja')
let $filename := concat($documentId, '.xml')
return
    if (not(xmldb:get-child-collections('/db') = 'autorska-prava')) then <missing/>
    else if (not(xmldb:get-child-collections($parent) = 'a1')) then <missing/>
    else if (not(xmldb:get-child-resources($requests) = $filename)) then <missing/>
    else util:exclusive-lock(doc(concat($requests, '/', $filename)),
        let $collection :=
            if (xmldb:get-child-collections($parent) = 'resenja') then $resolutions
            else xmldb:create-collection($parent, 'resenja')
        return
            if (xmldb:get-child-resources($collection) = $filename) then <duplicate/>
            else
                let $stored := xmldb:store($collection, $filename, parse-xml($xml))
                return if (exists($stored)) then <created/> else error()
    )
