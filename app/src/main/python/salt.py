# Decompiled with PyLingual (https://pylingual.io)
# Internal filename: '__'
# Bytecode version: 3.9.0beta5 (3425)
# Source timestamp: 1970-01-01 00:00:00 UTC (0)

import sys
import os
from timeit import default_timer
drn = ''
s = sys.argv
rep = '.'
l = len(s)
wtm = (-1)
fct = 0
lev = 4
cnt = 0
rec, cst = (False, False)
sgn = ''
tim = 0
act = 'Obfuscating smali code'
cur = ''
ruc = 'recursively '
sn = '\n'
dot = ''
def sexit(ex):
    sys.exit(ex)
def process(entry, cst, lev, sp, sg):
    # ***<module>.process: Failure: Different bytecode
    import random
    with open(entry, 'r', errors='surrogateescape') as f:
        content = f.readlines()
    with open(entry, 'w', errors='surrogateescape') as f:
        par, reg, mth, gn, md = (0, 0, 0, 0, 0)
        cnt = 0
        go, dsn = (False, False)
        p, q = (False, True)
        sn = '\n'
        ta = ('B', 'C', 'F', 'I', 'L', 'S', 'Z')
        tb = ('D', 'J')
        ks = '-int/2addr '
        tc = ['long', 'double', 'wide']
        tv = ['    move-', '    :', '    nop', '    .', '        ', '.']
        td = ['    move ', '    move/from16', '    move-object', '    const-class', '    const-wide', '    const-string']
        tn = ['    and' + ks, '    or' + ks, '    xor' + ks]
        to = tn + ['    shr' + ks, '    shl' + ks]
        ve = ''.join((chr(x ^ 15) for x in [124, 98, 96, 109, 122, 127, 126]))
        for line in content:
            nline = ''
            pline = ''
            lyne = ''
            if ' constructor ' in line:
                dsn = True
            if line.startswith('.method') and (not dsn or cst):
                    par = 0
                    l = ta[4]
                    x_ = line.split('(')[1].split(')')[0].split(';')
                    for i in range(len(x_)):
                        y_ = x_[i].split(l)[0]
                        x_[i] = y_ if x_[i] == y_ else y_ + l
                        par += sum((c_ in ta for c_ in x_[i])) + 2 * sum((c_ in tb for c_ in x_[i]))
                    go, rad = (True, False)
                    xline = ''
                    mth += 1
            if go:
                if line.startswith('    .locals'):
                    reg = int(line.split('locals ')[1])
                    if reg > 15:
                        reg = 15
                    if reg + par < 15:
                        rad = True
                        line = f'    .locals {reg + 1}' + sn
                if rad and line == sn:
                        if (sp or sg) and (not dsn):
                            if sp:
                                sf = sp
                                sp = ''
                                gn = mth
                            else:
                                if sg and mth - gn > 0:
                                        sf = sg
                                        sg = ''
                            if sf:
                                xline = f'    const-string v{reg}, \"{sf}\"' + 2 * sn
                                sf = ''
                        else:
                            if random.randint(0, 10) <= lev and q:
                                    xline = f"    const/4 v{reg}, 0x{random.randint(0, 7)}" + 2 * sn
                                    p = True
                        q = True
                if line.startswith('    :'):
                    p = False
                if rad and line.startswith('    const/4 ') and (not xline) and (random.randint(0, 10) <= lev):
                                if random.randint(0, 10) < 5:
                                    pline = line.split('x')[0] + 'x' + str(random.randint(0, 7)) + 2 * sn
                                    cnt += 1
                                else:
                                    if random.randint(0, 10) < 6:
                                        nline = sn + f"    move v{reg},{line.split(',')[0].split('const/4')[1]}" + sn
                                        p, q = (True, False)
                                        cnt += 1
                                    else:
                                        if p:
                                            if line.split(', ')[1][0].startswith('-'):
                                                nline = sn + f"{tn[random.randint(0, 2)]}v{reg},{line.split(',')[0].split('const/4')[1]}" + sn
                                                q = False
                                                cnt += 1
                                            else:
                                                nline = sn + f"{to[random.randint(0, 4)]}v{reg},{line.split(',')[0].split('const/4')[1]}" + sn
                                                q = False
                                                cnt += 1
                td = ['    move ', '    move/from16', '    move-object', '    const-class', '    const-wide', '    const-string']
                if any((line.startswith(c) for c in td)) and random.randint(0, 10) <= lev:
                        if line.startswith('    const-string'):
                            nline = sn + line
                            lyne = line[:(-1)].split(', \"')
                            if len(lyne) == 2:
                                lune = lyne[1][:(-1)].replace('\'', '/').replace('\"', '/').replace('\\', '/')
                                lune = list(lune)
                                random.shuffle(lune)
                                if len(lyne[1]) > 5:
                                    md %= 7
                                    lune.insert(md - len(lune) + 2, ve[md])
                                    lune = '\"' + ''.join(lune)[:(-1)] + '\"' + sn
                                    line = lyne[0] + ', ' + lune
                                    md += 1
                                else:
                                    lune = '\"' + ''.join(lune) + '\"' + sn
                                    line = lyne[0] + ', ' + lune
                        else:
                            nline = sn + line
                        cnt += 1
                tv = ['    move-', '    :', '    nop', '    .', '        ', '.']
                if any((line.startswith(c) for c in tv)):
                    pline = ''
                else:
                    if xline and (not line == sn):
                            pline = xline
                            xline = ''
                            cnt += 1
            if line.startswith('.end method'):
                go, dsn = (False, False)
                p, q = (False, True)
            f.write(pline + line + nline)
        return cnt
def purge(entry, _a, _b, _c, _d):
    # irreducible cflow, using cdg fallback
    # ***<module>.purge: Failure: Different control flow
    tq = ['    .line', '    .prologue', '.source', '    .local ', '    .end local', '    .restart', '    .param']
    cnt = 0
    with open(entry, 'r', errors='surrogateescape') as f:
        lis = list(f)
        content = ''.join(lis)
    if any((t in content for t in tq)):
        # NOTE: the decompiled version opened the file for 'w' here and
        # immediately closed it (with ... pass), which truncates the
        # file to empty, and then tried to f.write() further down after
        # that file handle was already closed -> it wiped .smali files
        # instead of cleaning them. The write now happens in the same
        # open() that does the writing, and `return cnt` is after the
        # loop instead of inside it (it used to bail out after the very
        # first matched line). The original .param/#-comment/.annotation
        # handling could not be reliably recovered by the decompiler
        # (it flagged this function as "irreducible cflow" itself), so
        # .param lines are now dropped like the other debug directives
        # instead of guessing at that logic.
        with open(entry, 'w', errors='surrogateescape') as f:
            for line in lis:
                if any((line.startswith(c) for c in tq)):
                    cnt += 1
                    continue
                f.write(line)
    return cnt
def run(target, operation, level, recursive, constructors):
    """
    Entry point Java actually calls:
        module.callAttr("run", path, "obf"|"pur", level, recursive, constructors)

    The decompiled file only had top-level script code driven by
    sys.argv (-r, -c, -p, -l, -s flags) and no 'run' attribute at all,
    so Chaquopy's callAttr("run", ...) would fail immediately with
    AttributeError. This replaces the argv parsing with the explicit
    parameters Java is actually passing, but keeps the same processing
    logic/messages as before.

    `target` must be a real filesystem path, not a content:// SAF tree
    Uri (os.walk/open can't read those) - resolve it to a path in Java
    before calling this.
    """
    global drn, fct, lev, cnt, rec, cst, sgn, tim, act

    out = []
    command = process

    cu = '  ' + 10 * ''.join((chr(x ^ 15) for x in [113, 79])) + ''.join((chr(x ^ 15) for x in [113])) + '  '
    k = ''.join((chr(x ^ 15) for x in [47, 92, 98, 96, 109, 47, 34, 47, 66, 96, 107, 47, 96, 109, 105, 122, 124, 108, 110, 123, 102, 96, 97, 47, 123, 96, 96, 99, 47, 121, 62, 33, 59, 47, 109, 118, 47, 68, 102, 125, 99, 102, 105]))
    kk = ''.join((chr(x ^ 15) for x in [5, 47, 92, 98, 96, 109, 47, 34, 47, 66, 96, 107, 47, 96, 109, 105, 122, 124, 108, 110, 123, 102, 96, 97, 47, 123, 96, 96, 99, 5, 47, 47, 47, 47, 47, 47, 47, 121, 62, 33, 59, 47, 109, 118, 47, 68, 102, 125, 99, 102, 105, 40, 5]))
    sp = cu + k + '\\\' ' + cu
    ll = '#############################'
    out.append(ll + kk + ll + sn)

    rec = bool(recursive)
    cst = bool(constructors)
    sgn = ''
    act = 'Obfuscating smali code'

    if operation == 'pur':
        command = purge
        rec = True
        act = 'Removing debug info'

    try:
        lev = int(level)
    except (TypeError, ValueError):
        lev = 4
    if lev < 0 or lev > 10:
        lev = 4

    if target.startswith('content://'):
        return ('Error: got a content:// URI, this script needs a real '
                'filesystem path. Resolve the folder Uri to a path in Java first.')

    drn = os.path.realpath(target)
    if not os.path.isdir(drn):
        return f'Folder not found: {drn}'

    fct = 0
    cnt = 0
    start_time = default_timer()
    try:
        out.append('- ' + act)
        if rec:
            for root, _, entries in os.walk(drn):
                for entry in entries:
                    ent = os.path.join(root, entry)
                    if os.path.splitext(entry)[1] == '.smali':
                        cnt += command(ent, cst, lev, sp, sgn)
                        fct += 1
        else:
            for entry in os.listdir(drn):
                if os.path.splitext(entry)[1] == '.smali':
                    ent = os.path.join(drn, entry)
                    cnt += command(ent, cst, lev, sp, sgn)
                    fct += 1
    except OSError as e:
        msg = str(e)
        msg = msg.split(']')[1] if ']' in msg else msg
        return f'\n {msg}'
    except Exception:
        return '\nOops something went wrong!!!'

    tim = default_timer() - start_time
    out.append('- Job completed\n')
    out.append(ll)
    if fct:
        out.append('- Process duration: ' + f'{round(tim, 2)}s')
        out.append('- Processed files: ' + f'{fct}')
        out.append('- Applied patches: ' + f'{cnt}')
        if tim > 0:
            out.append('- Patches per second: ' + f' {int(cnt / tim)}')
    else:
        out.append('  Nothing to patch here!!!')
    out.append(ll)

    return '\n'.join(out)