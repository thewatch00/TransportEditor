; Transport p01-10-city-5nodes-1000size-3degree-100mindistance-2trucks-2packagespercity-2008seed

(define (problem transport-p01-10-city-5nodes-1000size-3degree-100mindistance-2trucks-2packagespercity-2008seed)
(:domain transport)
(:objects
city-loc-1 - location
city-loc-2 - location
city-loc-3 - location
city-loc-4 - location
city-loc-5 - location
truck-1 - vehicle
truck-2 - vehicle
package-1 - package
package-2 - package
)
(:init
; 890,543 -> 748,385
(road city-loc-1 city-loc-3)
(= (road-length city-loc-1 city-loc-3) 22)
(= (fuel-demand city-loc-1 city-loc-3) 43)
; 890,543 -> 912,799
(road city-loc-1 city-loc-4)
(= (road-length city-loc-1 city-loc-4) 26)
(= (fuel-demand city-loc-1 city-loc-4) 52)
; 890,543 -> 977,899
(road city-loc-1 city-loc-5)
(= (road-length city-loc-1 city-loc-5) 37)
(= (fuel-demand city-loc-1 city-loc-5) 74)
; 384,50 -> 748,385
(road city-loc-2 city-loc-3)
(= (road-length city-loc-2 city-loc-3) 50)
(= (fuel-demand city-loc-2 city-loc-3) 99)
; 748,385 -> 890,543
(road city-loc-3 city-loc-1)
(= (road-length city-loc-3 city-loc-1) 22)
(= (fuel-demand city-loc-3 city-loc-1) 43)
; 748,385 -> 384,50
(road city-loc-3 city-loc-2)
(= (road-length city-loc-3 city-loc-2) 50)
(= (fuel-demand city-loc-3 city-loc-2) 99)
; 748,385 -> 912,799
(road city-loc-3 city-loc-4)
(= (road-length city-loc-3 city-loc-4) 45)
(= (fuel-demand city-loc-3 city-loc-4) 89)
; 912,799 -> 890,543
(road city-loc-4 city-loc-1)
(= (road-length city-loc-4 city-loc-1) 26)
(= (fuel-demand city-loc-4 city-loc-1) 52)
; 912,799 -> 748,385
(road city-loc-4 city-loc-3)
(= (road-length city-loc-4 city-loc-3) 45)
(= (fuel-demand city-loc-4 city-loc-3) 89)
; 912,799 -> 977,899
(road city-loc-4 city-loc-5)
(= (road-length city-loc-4 city-loc-5) 12)
(= (fuel-demand city-loc-4 city-loc-5) 24)
; 977,899 -> 890,543
(road city-loc-5 city-loc-1)
(= (road-length city-loc-5 city-loc-1) 37)
(= (fuel-demand city-loc-5 city-loc-1) 74)
; 977,899 -> 912,799
(road city-loc-5 city-loc-4)
(= (road-length city-loc-5 city-loc-4) 12)
(= (fuel-demand city-loc-5 city-loc-4) 24)
(at package-1 city-loc-3)
(= (package-size package-1) 23)
(at package-2 city-loc-4)
(= (package-size package-2) 55)
(has-petrol-station city-loc-1)
(at truck-1 city-loc-3)
(ready-loading truck-1)
(= (fuel-left truck-1) 424)
(= (fuel-max truck-1) 424)
(at truck-2 city-loc-4)
(ready-loading truck-2)
(= (fuel-left truck-2) 424)
(= (fuel-max truck-2) 424)
)
(:goal (and
(preference delivery-1 (at package-1 city-loc-2))
(preference delivery-2 (at package-2 city-loc-3))
))
(:metric maximize
(- 260
(+ (total-time)
(* (is-violated delivery-1) 115)
(* (is-violated delivery-2) 145)
)
)
)
)
