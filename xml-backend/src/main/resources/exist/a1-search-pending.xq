xquery version "3.1";
declare namespace a1 = "https://example.org/autorska-prava/a1";
declare namespace r = "https://example.org/autorska-prava/resenje";
declare namespace l = "https://example.org/autorska-prava/a1-lista";
declare variable $q as xs:string external;

let $requests := '/db/autorska-prava/a1'
let $resolutions := '/db/autorska-prava/resenja'
let $parentExists := xmldb:get-child-collections('/db') = 'autorska-prava'
let $children := if ($parentExists) then xmldb:get-child-collections('/db/autorska-prava') else ()
let $search := lower-case(normalize-space($q))
return <l:listaZahteva>{
    if ($children = 'a1') then
        let $decisions := if ($children = 'resenja') then collection($resolutions)/r:resenje else ()
        for $request in collection($requests)/a1:zahtevA1
        where empty($decisions[r:zahtevRef = $request/@id]) and (
            $search = '' or
            (some $element in $request//*[not(*)]
             satisfies contains(lower-case(normalize-space(string($element))), $search))
        )
        return $request
    else ()
}</l:listaZahteva>
