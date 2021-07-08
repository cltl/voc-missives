# Location names
## compositional or given-name?
If it looks like a given name, label it as an entity

* *[Javas Oostcust]LOC*
* *[Bataviase Ommelanden]LOC*
* *[Kust van Coromandel]LOC*
* *de [Specerij eilanden]LOC*


But
* *het zuiden der [Philippijnen]LOC*
* *de westkust van [Hitu]LOC*
* *het eiland [Saparua]LOC*

# Combined entities
Stay close to words?
* *[Portugees]LOCderiv [Timor]LOC*
* *de [Engelse]LOCderiv [Compagnie]ORG*
* *de [Compagnie]ORG van [St. Malo]LOC*
* *de [Kamers]ORG [Amsterdam]LOC, [Hoorn]LOC en [Enkhuizen]LOC*

or favor direct entity identification?
* *de [Camer Rotterdam]ORG*

-> Stay close to the words for maximum information

but only for common compounds prone to ellipsis, like Kamer and Compagnie
* *de [Rade van India]_ORG*

# LOC or GPE?
only assign GPE to locations in agent/patient role. The distinction between owner/beneficiary on one hand and source/direction on the other hand being hard to make reliably.

Agent:
* *de eis ... van [Banda]GPE*
* *door [Banda]GPE getrokken*
* *door [Makassar]GPE gesteunde*

Beneficiary:
* *[Calicoylan]GPE is door ons ... bevoordeeld*
* *alcatieven voor [Japan]GPE*

Trading agent:
* *[Formosa]GPE hadde 2500 picol gelevert*
* *d'Inkompsten van [Batavia]GPE*
* *Kosten van [Banda]GPE*
* *handel met [Perzië]GPE*
* *voor rekeninge van [Zeeland]GPE*



Warfaring:
* *oorlogh tusschen ons ende [Bantham]GPE*
* *het bestand met [Portugal]GPE*
* *deze strijd tegen [Camatica]GPE*


but:
* *Kosten in [Banda]LOC*
* *berichten uit [China]LOC*
* *berichten van [Banda]LOC*
* *handel van [Makassar]LOC*
* *de ... schepen waren van [Madras]LOC*

# political actors
This was a source of confusion under annotators: only named entities are labeled.

* *hof van [Cheribon]LOC*
* *gouvernement [Amboina]LOC*

# Booktitles
Titles are not entities, but entity names contained in titles are labeled with the corresponding entity type:

- *[Borneo]LOC, p. 8.*
- *bij [Valentijn]PER, Beschrijvingen van [Amboina]LOC, p. 102*


# OTH / REL
This label was mainly used for religious group names, derived forms thereof, and group names not related to a location:

* *de [heidense]OTH ... [Moslimse]OTH kustbewoners ... die hen ... [Alfoeren]OTH, boerenkinkels, noemden

We replace it by REL, RELderiv, and RELpart. Group names not related to a location or religion are assimilated to derived location
 forms LOCderiv

* *de [heidense]RELderiv ... [Moslimse]RELderiv kustbewoners ... die hen ... [Alfoeren]LOCderiv, boerenkinkels, noemden
* *concurrentie van [Engelsen]LOCderiv en [Moren]RELderiv