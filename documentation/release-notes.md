# Release notes

Overzicht van wijzigingen per versie van de Suwinet-plugin.

## 3.0.1

Valtimo bijgewerkt naar versie 13.41.0.

## 3.0.0
Geschikt gemaakt voor Valtimo v13 en bijgewerkt naar de laatste v12-wijzigingen, met fixes voor de Angular-build na een Valtimo-dependency-update.

## 1.14.12-1
Versie van frontend en backend gelijkgetrokken.

## 1.14.12
Foutafhandeling toegevoegd voor datetime-parse-excepties.

## 1.14.11
Nullcheck toegevoegd in de bijstandsregelingen-service voor ontbrekende partnergegevens.

## 1.14.10
WS-Addressing toegevoegd met uitgeschakelde messageId-checks.

## 1.14.9
`adresBrp` wordt niet langer meegegeven wanneer deze leeg is.

## 1.14.8
Datums gebruikt waar mogelijk, behalve voor de vorderingDTO.

## 1.14.7
Fixes voor de Suwinet Gateway-koppelingen DH, SVB en UWV.

## 1.14.6
Datumformattering toegevoegd.

## 1.14.5
A-nummer wordt nu als lege string teruggegeven wanneer niet aanwezig.

## 1.14.4
`ingangsdatumHuwelijk` toegevoegd aan BRP-gegevens.

## 1.14.3
Veld `aNummer` hernoemd naar `anummer` om Java-naamgevingsconventies te volgen.

## 1.14.2
Kleine optimalisaties in het ophalen van gegevens.

## 1.14.1
BRP-velden uitgebreid voor Socrates.

## 1.14.0
BRP-velden verder uitgebreid voor Socrates.

## 1.13.3
Eerdere BRP-uitbreiding teruggedraaid.

## 1.13.2
Logging toegevoegd en retourtypes gecorrigeerd.

## 1.13.1
Ophalen van `geslachtsnaamPartner` vereenvoudigd.

## 1.13.0
BRP-info uitgebreid en aangesloten op een generieke Suwinet-simulator.

## 1.12.2
Bugfix in de Suwinet-plugin.

## 1.12.1
Aanvullende bugfix in de Suwinet-plugin.

## 1.12.0
BPMN-exceptions worden nu doorgegeven voor betere foutafhandeling in processen.

## 1.11.0
Uitgebreidere foutafhandeling in de BRP-info-service.

## 1.10.1
Dubbele quotes verwijderd uit de Suwinet SOAPAction-header.

## 1.10.0
DocumentWriterService gesplitst in een Suwinet- en een DH-implementatie.

## 1.8.0
WS-Addressing toegevoegd.

## 1.7.1
Gevoelige header `x-opentunnel-api-key` toegevoegd aan de masking-configuratie.

## 1.7.0
Begin- en einddatum van Inkomstenverhouding toegevoegd aan UWV-gegevens.

## 1.6.0
Suffix `Suwinet` toegevoegd om naamgeving consistent te maken.

## 1.5.7 / 1.5.6
Ophalen van historische verblijfplaats toegevoegd.

## 1.4.6
`Authorization`-header wordt nu gemaskeerd in logging.

## 1.4.5
Aanvullende logging en headers toegevoegd aan Suwinet-requests.

## 1.4.4
Pad voor studiefinanciering rechtgezet.

## 1.4.3
Versiebump voor publicatie.

## 1.4.2
Foutafhandeling rond suwinet-connect-fouten verbeterd, inclusief test-fixes.

## 1.4.1
Configuratie toegevoegd voor de bijstandsregelingen-service.

## 1.4.0
Versiebump met aanvullende fixes.

## 1.3.0
BPMN-fouten toegevoegd voor ontbrekende BSN bij BRP-bevraging.

## 1.2.1
Start van implementatie van bijstandsregelingen.

## 1.1.1
Standaard maximumleeftijd voor kinderen toegevoegd als configureerbare waarde.

## 1.1.0
Backend-versie van de plugin gecorrigeerd.

## 1.0.1
Fixes voor Spotler, Amsterdam Email API en Suwinet.

## 1.0.0
Eerste publieke release: burger- en bedrijfsgegevens ophalen via Suwinet (BRP, DUO, Kadaster, RDW, SVB en UWV).
