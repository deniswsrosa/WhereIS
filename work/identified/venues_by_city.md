# Venues & flight connections by city (observed in-game)

Recorded during the DOSBox capture grind (case started at Moroni, detective
GUMSHOE, 2026-07-23). NOTE: each city always has an **Airport** plus two others;
**Stock Exchange** and **Riverfront** are the two rarest venue types and did not
appear in any city below. **Venues are DYNAMIC: re-rolled per case, fixed within a case.** Proof: last
session's Bamako = Sport Club/Riverfront/Foreign Ministry; this case's Bamako =
Bank/Harbor/Marketplace. So this table is valid only for the current case.

| City | Venues | Flights to |
|---|---|---|
| Moroni | Hotel, Airport, Marketplace | Port Moresby, Bamako, Athens |
| Bamako | Bank, Harbor, Marketplace | Oslo, Moroni, Kigali |
| Kigali | Museum, Airport, Palace | Bangkok, Kathmandu, Bamako, Cairo |
| Cairo | Museum, Airport, Foreign Ministry | Montreal, Singapore, Colombo, Kigali |
| Montreal | Bank, Airport, Palace | Cairo, London, Reykjavik, Budapest |
| Budapest | Bank, Airport, Marketplace | Peking, Buenos Aires, Oslo, Montreal |
| Buenos Aires | Sport Club, Harbor, Foreign Ministry | Mexico City, Budapest, Sydney, New York |
| Mexico City | Hotel, Airport, Foreign Ministry | Buenos Aires, Lima, Port Moresby |
| Lima | Museum, Airport, Palace | London, Mexico City, Istanbul, San Marino |
| London | Museum, Harbor, Foreign Ministry | Lima, Montreal, New Delhi |
| New Delhi | Museum, Airport, Palace | New York, Bangkok, London |
| New York | Hotel, Harbor, Marketplace | New Delhi, Rome, Moscow, Buenos Aires |
| Rome | Sport Club, Harbor, Foreign Ministry | Reykjavik, New York, San Marino |
| Reykjavik | Bank, Airport, Marketplace | Rome, Athens, Montreal |
| Athens | Sport Club, Airport, Palace | Baghdad, Reykjavik, Moroni, Sydney |
| Baghdad | Hotel, Airport, Marketplace | Athens, Rio de Janeiro, Kathmandu |
| Rio de Janeiro | Sport Club, Airport, Palace | (Baghdad, + TBD) |

## Witness rolled per venue visited (this case)
- Moroni Marketplace → Urchin
- Bamako Marketplace → **Hawker** ✅ (captured)
- Kigali Palace → Privy Councillor
- Cairo Museum → Museum guard
- Montreal Palace → **Palace guard** ✅ (captured)
- Mexico City Hotel → Bellhop
- Lima Museum → Museum guard
- New Delhi Museum → Curator
- New York Hotel → Bellhop
- Rome Sport Club → Waiter
- Reykjavik Bank → Vice President

## Not yet found this case
- **Stock Exchange** venue (→ Analyst / Trader) — 0 sightings in 14 cities
- **Riverfront** venue (→ Tugboat captain) — 0 sightings in 14 cities
- Unexplored cities reachable: Athens, Oslo, Port Moresby, Bangkok, Kathmandu,
  Singapore, Colombo, Peking, Sydney, Istanbul, San Marino, Moscow

## 2026-08-06 session (Rookie route-length audit, opportunistic Stock Exchange hunt)
Played 3 full fresh Rookie cases (new detective each time) end-to-end, checking the
investigate-tool venue triplet in every city visited (11 city visits, 33 venue-slot
observations). **Still 0 Stock Exchange sightings** — historical count is now 0/25ish
cities. Riverfront *did* show up multiple times this session (already captured/wired,
per ANDROID_FIDELITY_TODO §6), confirming it's no longer the bottleneck — Stock Exchange
remains the only missing venue type.

Venue triplets observed (case/city — venues):
- Case1 Bangkok — Sport Club, Riverfront, Marketplace
- Case1 Moroni — Museum, Airport, Marketplace
- Case1 Moscow — Hotel, Library, Foreign Ministry
- Case1 Peking — Bank, Riverfront, Marketplace
- Case1 Paris — Hotel, Library, Marketplace
- Case2 Moscow — Museum, Airport, Marketplace
- Case2 Peking — Museum, Riverfront, Foreign Ministry
- Case2 Paris — Museum, Library, Marketplace
- Case2 Lima — Hotel, Library, Foreign Ministry
- Case2 Budapest — Bank, Library, Foreign Ministry
- Case3 Baghdad — Bank, Library, Foreign Ministry
- Case3 Oslo — Bank, Harbor, Foreign Ministry
- Case3 Buenos Aires — Bank, Harbor, Foreign Ministry
- Case3 Colombo — Museum, Library, Palace
- Case3 Istanbul — Bank, Airport, Palace

Given the 0/25ish hit rate, Stock Exchange is likely gated behind a subset of cities
not visited yet (candidates not seen this session or last: Athens, Port Moresby,
Bangkok's other case rolls, Kathmandu, Singapore, Sydney, San Marino, Cairo, Kigali,
Mexico City, New Delhi, New York, Rome, Reykjavik, Rio de Janeiro, London, Bamako,
Mont Saint-Michel-tier world-database cities not yet reachable at Rookie rank). Next
session should target those specifically rather than re-rolling already-checked cities.
