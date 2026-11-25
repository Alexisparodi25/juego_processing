#!/usr/bin/env python3
"""Descarga de WikiDex (API MediaWiki) los nombres y descripciones en español de movimientos,
habilidades y objetos, y los guarda como pokefuego/data/es_*.tsv  (id inglés, nombre, descripción)."""
import json, re, sys, urllib.parse, urllib.request, os

OUT = os.path.join(os.path.dirname(__file__), '..', 'pokefuego', 'data')

def wikitext(page):
    url = 'https://www.wikidex.net/api.php?' + urllib.parse.urlencode(
        {'action': 'parse', 'page': page, 'prop': 'wikitext', 'format': 'json'})
    req = urllib.request.Request(url, headers={'User-Agent': 'pokefuego-datos/1.0'})
    return json.load(urllib.request.urlopen(req, timeout=60))['parse']['wikitext']['*']

def toid(s): return re.sub(r'[^a-z0-9]', '', s.lower())

def clean(s):
    s = re.sub(r'<ref[^>]*/>', '', s)
    s = re.sub(r'<ref[^>]*>.*?</ref>', '', s, flags=re.S)
    s = re.sub(r'\{\{[^{}]*\}\}', '', s)
    s = re.sub(r'\[\[(?:[^\]|]*\|)?([^\]]*)\]\]', r'\1', s)
    s = s.replace("'''", '').replace("''", '').replace('<br>', ' ').replace('<br/>', ' ')
    s = re.sub(r'<[^>]+>', '', s)
    return re.sub(r'\s+', ' ', s).strip()

def link_name(cell):
    """Nombre en español a partir de [[A/B]] o [[A|B]] (se prefiere el nombre actual, tras '/')."""
    m = re.search(r"\[\[([^\]]+)\]\]", cell)
    if not m: return clean(cell.split('<br>')[0])
    t = m.group(1)
    if '|' in t: t = t.split('|')[-1]
    if '/' in t: t = t.split('/')[0]
    return t.strip()

def rows(w):
    for blk in w.split('\n|-')[1:]:
        cells = [c.strip() for c in re.split(r'\n\|(?!\|)', '\n' + blk) if c.strip()]
        yield cells

def english(cell):
    m = re.findall(r"''([^']+)''", cell)
    return m[-1] if m else None

def moves():
    out = {}
    for c in rows(wikitext('Lista de movimientos')):
        if len(c) < 4 or not re.match(r'^\d{3,4}$', c[0]): continue
        en = english(c[1])
        if not en: continue
        out[toid(en)] = (link_name(c[1]), clean(c[3]))
    return out

def abilities():
    out = {}
    for c in rows(wikitext('Lista de habilidades')):
        if len(c) < 6 or not re.match(r'^\d{3}$', c[0]): continue
        en = english(c[3])
        if not en: continue
        out[toid(en)] = (clean(c[2]), clean(c[5]))
    return out

def items():
    out = {}
    for c in rows(wikitext('Lista de objetos')):
        for i, cell in enumerate(c):
            if "''" in cell and '<br>' in cell and "'''" in cell:
                en = english(cell)
                desc = clean(c[i + 2]) if i + 2 < len(c) else ''
                if en: out[toid(en)] = (link_name(cell), desc)
                break
    return out

def gmax(moves_es):
    """Movimientos G-Max: se enlazan por especie (WikiDex los lista como 'Especie Gigamax')."""
    species_to_move = {}
    for l in open(os.path.join(OUT, 'moves.tsv'), encoding='utf-8'):
        c = l.rstrip('\n').split('\t')
        if len(c) > 34 and c[34] not in ('', 'Max'): species_to_move[toid(c[34])] = c[0]
    w = wikitext('Movimiento Gigamax')
    for line in w.split('\n|-'):
        m = re.search(r"<br>\[\[([^\]|\[]+)\]\]\s*\|\|\s*\{\{.*?\}\}\s*\|\|\s*(.*?)\s*\|\|\s*(.*)", line.replace('\n', ' '), re.S)
        if not m: continue
        name = m.group(1).strip(); desc = clean(m.group(2))
        for sp in re.findall(r"\[\[([^\]|]+?) Gigamax\]\]", m.group(3)):
            mid = species_to_move.get(toid(sp))
            if mid: moves_es[mid] = (name, desc)
    return moves_es

def save(name, d):
    with open(os.path.join(OUT, name), 'w', encoding='utf-8') as f:
        for k, (n, desc) in sorted(d.items()):
            f.write('\t'.join([k, n.replace('\t', ' '), desc.replace('\t', ' ')]) + '\n')
    print(name, len(d))

if __name__ == '__main__':
    save('es_moves.tsv', gmax(moves())); save('es_abilities.tsv', abilities()); save('es_items.tsv', items())
