xquery version "3.1";
declare namespace a1 = "https://example.org/autorska-prava/a1";
declare namespace r = "https://example.org/autorska-prava/resenje";
declare namespace p = "https://example.org/autorska-prava/a1-pretraga";
declare variable $q as xs:string external;

let $requests := '/db/autorska-prava/a1'
let $resolutions := '/db/autorska-prava/resenja'
let $parentExists := xmldb:get-child-collections('/db') = 'autorska-prava'
let $children := if ($parentExists) then xmldb:get-child-collections('/db/autorska-prava') else ()
let $search := lower-case(normalize-space($q))
return <p:rezultatiPretrage>{
    if ($children = 'a1' and $children = 'resenja') then
        for $request in collection($requests)/a1:zahtevA1
        let $approved := collection($resolutions)/r:resenje[r:odobrenje][r:zahtevRef = $request/@id]
        where exists($approved) and (
            $search = '' or
            (some $element in $request//*[not(*)]
             satisfies contains(lower-case(normalize-space(string($element))), $search))
        )
        return <p:delo zahtevId="{string($request/@id)}">
            <p:naslov>{string($request/a1:podaciGradjanina/a1:delo/a1:naslov)}</p:naslov>
            <p:vrsta>{string($request/a1:podaciGradjanina/a1:delo/a1:vrsta)}</p:vrsta>
        </p:delo>
    else ()
}</p:rezultatiPretrage>
