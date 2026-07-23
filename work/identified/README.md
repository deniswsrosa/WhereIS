# Identified places & witnesses — COMPLETE

Canonical rosters are the game's own tables, extracted from the EXE strings
(`work/carmen_exe_strings.txt`). All art below is sourced from the original game
(in-game DOSBox captures + two reference screenshots for the final Riverfront /
Analyst).

## Venues — 12 / 12 ✅ (`venues/`)

Canonical venue table (EXE offsets 1a2b1–1a313):
Bank, Hotel, Museum, Sport Club, Library, Airport, Harbor, Riverfront,
Palace, Stock Exchange, Marketplace, Foreign Ministry.

Files: airport, bank, foreign_ministry, harbor, hotel, library, marketplace,
museum, palace, **riverfront**, sport_club, stock_exchange.

## Witnesses — 35 / 35 occupations ✅ (`witnesses/`)

Canonical occupation table (EXE offsets 1a334–1a4bd), in game order:
Bank Guard, Teller, Hotel manager, Bellhop, House detective, Museum guard,
Docent, Curator, Waiter, Tennis pro, Bartender, Circulation clerk,
Reference librarian, Archivist, Pilot, Flight attendant, Baggage clerk,
Sailor, Harbor Master, Customs officer, Stevedore, Tugboat captain,
Sailor's parrot, Palace guard, Soldier, Privy Councillor, **Analyst**, Trader,
Messenger, Hawker, Street merchant, Urchin, Under Secretary, Attache,
Ambassador. Plus the special `$Vice President` (also saved).

36 files = all 35 occupations + vice_president.

## Provenance of the last four
- **Hawker** — DOSBox capture, Bamako Marketplace
- **Palace guard** — DOSBox capture, Montreal Palace
- **Riverfront** (venue) — cropped from a Paris venue-popup reference screenshot
- **Analyst** (witness) — cropped from a Paris reference screenshot

See `venues_by_city.md` for the per-case venue/flight-network log gathered during
the capture grind, and memory `carmen-dat-archive-format.md` for the (partially
reversed) native `.DAT` extraction path.
