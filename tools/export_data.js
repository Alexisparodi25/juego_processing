// Exporta TODOS los datos de Pokémon (especies, formas, megas, gigamax, movimientos,
// habilidades, objetos, aprendizajes, tabla de tipos) a archivos TSV en pokefuego/data.
//
// Uso:   cd tools && npm install && node export_data.js
// Fuente: paquete npm @pkmn/dex (datos de Pokémon Showdown, generaciones 1-9).
const fs = require('fs');
const path = require('path');
const {Dex} = require('@pkmn/dex');

const OUT = path.join(__dirname, '..', 'pokefuego', 'data');
fs.mkdirSync(OUT, {recursive: true});
const d = Dex.forGen(9);

const BAD = new Set(['CAP', 'Custom']);
const ok = x => x.exists && !BAD.has(x.isNonstandard);
const catCode = c => c === 'Physical' ? 'P' : c === 'Special' ? 'S' : 'N';
const clean = s => String(s == null ? '' : s).replace(/[\t\r\n]+/g, ' ').trim();
const boostStr = b => b ? Object.entries(b).map(([k, v]) => k + ':' + v).join('/') : '';
const write = (name, rows) => fs.writeFileSync(path.join(OUT, name), rows.map(r => r.map(clean).join('\t')).join('\n') + '\n');

// ---------------- Tipos ----------------
const types = [...d.types.all()].filter(t => t.name !== '???' && t.name !== 'Stellar');
const names = types.map(t => t.name);
const trows = [['atk\\def'].concat(names)];
for (const a of names) {
  const row = [a];
  for (const t of types) {
    const c = t.damageTaken[a];
    row.push(c === 1 ? 2 : c === 2 ? 0.5 : c === 3 ? 0 : 1);
  }
  trows.push(row);
}
write('types.tsv', trows);

// ---------------- Especies ----------------
const species = [...d.species.all()].filter(ok);
const sRows = [];
const tagOf = s => (s.tags || []).map(t => t.replace(/\s+/g, '')).join('|');
for (const s of species) {
  const evoInfo = (s.evos || []).map(n => {
    const e = d.species.get(n);
    return [e.evoLevel || '', e.evoType || '', e.evoItem || '', e.evoCondition || '', e.evoMove || ''].join(':').replace(/\|/g, '/');
  }).join('|');
  const isGmax = s.name.endsWith('-Gmax');
  const flags = [];
  if (s.isMega) flags.push('mega');
  if (s.isPrimal) flags.push('primal');
  if (isGmax) flags.push('gmax');
  if (s.isCosmeticForme) flags.push('cosmetic');
  if (s.battleOnly && !s.isMega && !s.isPrimal && !isGmax) flags.push('battleonly');
  if (s.cannotDynamax) flags.push('nodmax');
  if (s.isNonstandard === 'Past') flags.push('past');
  const tag = tagOf(s);
  sRows.push([
    s.id, s.name, s.num, s.gen, s.types[0], s.types[1] || '',
    s.baseStats.hp, s.baseStats.atk, s.baseStats.def, s.baseStats.spa, s.baseStats.spd, s.baseStats.spe,
    s.abilities[0] || '', s.abilities[1] || '', s.abilities.H || '',
    s.baseSpecies || s.name, s.forme || '', flags.join('|'),
    (s.requiredItems || (s.requiredItem ? [s.requiredItem] : [])).join('|'),
    s.requiredMove || '', typeof s.battleOnly === 'string' ? s.battleOnly : (s.battleOnly || []).join('|'),
    (s.evos || []).join('|'), evoInfo, s.prevo || '', s.weightkg, tag,
  ]);
}
write('species.tsv', sRows);

// ---------------- Movimientos ----------------
const moves = [...d.moves.all()].filter(m => ok(m) && m.isNonstandard !== 'Unobtainable' && m.isNonstandard !== 'Gmax' && !m.isZ);
const mRows = [];
for (const m of moves) {
  const sec = (m.secondaries && m.secondaries[0]) || m.secondary || null;
  let selfB = '';
  let tgtB = '';
  if (m.boosts) { if (m.target === 'self') selfB = boostStr(m.boosts); else tgtB = boostStr(m.boosts); }
  let selfAfter = m.self && m.self.boosts ? boostStr(m.self.boosts) : '';
  let selfAfterChance = 100;
  let secChance = 0, secStatus = '', secVol = '', secBoost = '';
  if (sec) {
    if (sec.self && sec.self.boosts) { selfAfter = boostStr(sec.self.boosts); selfAfterChance = sec.chance || 100; }
    else {
      secChance = sec.chance || 100;
      secStatus = sec.status || '';
      secVol = sec.volatileStatus || '';
      secBoost = boostStr(sec.boosts);
    }
  }
  const flags = Object.keys(m.flags || {}).join('|');
  const hasSelfStatus = m.self && m.self.volatileStatus ? m.self.volatileStatus : '';
  mRows.push([
    m.id, m.name, m.type, catCode(m.category), m.basePower, m.accuracy === true ? 0 : m.accuracy, m.pp, m.priority,
    m.critRatio || 1, m.target, flags,
    m.status || '', m.volatileStatus || hasSelfStatus, selfB, tgtB,
    secChance, secStatus, secVol, secBoost, selfAfter, selfAfterChance,
    m.drain ? m.drain.join('/') : '', m.recoil ? m.recoil.join('/') : '', m.heal ? m.heal.join('/') : '',
    m.multihit ? (Array.isArray(m.multihit) ? m.multihit.join('-') : m.multihit + '-' + m.multihit) : '',
    m.selfdestruct ? 1 : 0, m.ohko ? 1 : 0, m.weather || '', m.terrain || '', m.sideCondition || '',
    m.forceSwitch ? 1 : 0, m.selfSwitch ? 1 : 0, m.damage || '',
    m.maxMove ? m.maxMove.basePower : '', m.isMax ? (m.isMax === true ? 'Max' : m.isMax) : '',
    m.pseudoWeather || '', m.slotCondition || '', m.breaksProtect ? 1 : 0,
    m.overrideOffensiveStat || '', m.overrideDefensivePokemon || '', m.ignoreDefensive ? 1 : 0,
    m.willCrit ? 1 : 0, m.shortDesc || m.desc || '',
  ]);
}
// Movimientos Max y G-Max (para el Dynamax)
for (const m of d.moves.all()) {
  if (!m.exists || !m.isMax) continue;
  mRows.push([
    m.id, m.name, m.type, catCode(m.category), m.basePower, 0, 10, 0, 1, 'adjacentFoe', '', '', '', '', '', 0, '', '', '', '', 100, '', '', '', '', 0, 0, '', '', '', 0, 0, '', '', m.isMax === true ? 'Max' : m.isMax, '', '', 0, '', '', 0, 0, m.shortDesc || '',
  ]);
}
write('moves.tsv', mRows);

// ---------------- Habilidades ----------------
write('abilities.tsv', [...d.abilities.all()].filter(ok).map(a => [a.id, a.name, a.gen, a.rating, a.shortDesc || a.desc]));

// ---------------- Objetos ----------------
const items = [...d.items.all()].filter(i => ok(i) && i.isNonstandard !== 'Unobtainable');
write('items.tsv', items.map(i => {
  const mega = i.megaStone ? (typeof i.megaStone === 'string' ? i.megaStone : Object.values(i.megaStone)[0]) : '';
  const user = i.itemUser ? i.itemUser.join('|') : '';
  return [
    i.id, i.name, i.gen, mega, user, i.isBerry ? 1 : 0, i.isPokeball ? 1 : 0, i.onPlate || '', i.onMemory || '', i.onDrive || '',
    i.fling ? i.fling.basePower : '', i.naturalGift ? i.naturalGift.type : '', i.naturalGift ? i.naturalGift.basePower : '',
    i.isChoice ? 1 : 0, i.shortDesc || i.desc || '',
  ];
}));

// ---------------- Aprendizajes ----------------
const lrows = [];
const moveIds = new Set(moves.map(m => m.id));
(async () => {
  for (const s of species) {
    let ls = await d.learnsets.get(s.id);
    let src = s;
    // formas sin aprendizaje propio heredan de la especie base
    let guard = 0;
    while ((!ls || !ls.learnset || !Object.keys(ls.learnset).length) && guard++ < 3) {
      const baseName = src.changesFrom || (typeof src.battleOnly === 'string' ? src.battleOnly : '') || src.baseSpecies;
      if (!baseName || baseName === src.name) break;
      src = d.species.get(baseName);
      ls = await d.learnsets.get(src.id);
    }
    if (!ls || !ls.learnset) { lrows.push([s.id, '', '']); continue; }
    const lv = [], other = [];
    for (const [mv, codes] of Object.entries(ls.learnset)) {
      if (!moveIds.has(mv)) continue;
      let best = -1, bestGen = -1;
      let any = false;
      for (const c of codes) {
        const g = parseInt(c[0], 10);
        if (c[1] === 'L') { if (g > bestGen) { bestGen = g; best = parseInt(c.slice(2), 10); } }
        else if (c[1] === 'M' || c[1] === 'T' || c[1] === 'E' || c[1] === 'S' || c[1] === 'R' || c[1] === 'D') any = true;
      }
      if (best >= 0) lv.push(best + ':' + mv);
      else if (any) other.push(mv);
    }
    lv.sort((a, b) => parseInt(a) - parseInt(b));
    lrows.push([s.id, lv.join(','), other.join(',')]);
  }
  write('learnsets.tsv', lrows);
  const stat = n => console.log(n, fs.statSync(path.join(OUT, n)).size + ' bytes');
  ['types.tsv', 'species.tsv', 'moves.tsv', 'abilities.tsv', 'items.tsv', 'learnsets.tsv'].forEach(stat);
  console.log('especies', sRows.length, 'movs', mRows.length, 'habs', abilitiesCount(), 'objetos', items.length);
})();
function abilitiesCount() { return [...d.abilities.all()].filter(ok).length; }
