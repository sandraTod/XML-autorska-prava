xquery version "3.1";
declare namespace a1 = "https://example.org/autorska-prava/a1";
declare namespace l = "https://example.org/autorska-prava/a1-lista";
declare variable $vlasnikId as xs:string external;
let $collection := '/db/autorska-prava/a1'

let $parentExists := xmldb:get-child-collections('/db') = 'autorska-prava'
let $exists := if ($parentExists) then xmldb:get-child-collections('/db/autorska-prava') = 'a1' else false()
return <l:listaZahteva>{
    if ($exists) then
        collection($collection)/a1:zahtevA1[@vlasnikId = $vlasnikId]
    else ()
}</l:listaZahteva>
