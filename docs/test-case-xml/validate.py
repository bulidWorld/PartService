#!/usr/bin/env python3
"""Offline validation for generated task XML; never sends HTTP requests."""
import argparse
import ast
import builtins
import json
import re
from pathlib import Path
from lxml import etree

ROOT = Path(__file__).resolve().parent
SOURCE = ROOT.parent / 'test-case'
TYPES = {'string': str, 'integer': int, 'number': (int, float), 'boolean': bool,
         'object': dict, 'array': list}
ASSERTION_BUILTINS = set('abs all any bool dict enumerate filter float int isinstance len list map max min range reversed round set sorted str sum tuple zip Exception ValueError TypeError KeyError IndexError AssertionError'.split())


def validate(path, schema):
    errors = []
    def error(msg):
        errors.append(f'{path.name}: {msg}')
    parser = etree.XMLParser(resolve_entities=False, no_network=True)
    try:
        doc = etree.parse(str(path), parser)
        if doc.docinfo.doctype:
            error('DTD is forbidden')
        schema.assertValid(doc)
    except (etree.XMLSyntaxError, etree.DocumentInvalid) as exc:
        error(str(exc))
        return errors
    task = doc.getroot()
    declared = {}
    if path.stat().st_size > 5 * 1024 * 1024:
        error('file exceeds 5 MiB')
    if len(task.findall('.//taskStep')) > 1000:
        error('task exceeds 1000 nodes')

    def require(name, available, context):
        if not name or name not in available:
            error(f'{context}: undeclared/forward parameter {name!r}')

    def declare(pair, available):
        name = pair.findtext('name')
        if name in declared:
            error(f'duplicate parameter declaration {name}')
        declared[name] = pair.get('valueType', 'string')
        available.add(name)

    def literal(pair):
        typ = pair.get('valueType', 'string')
        text = pair.findtext('value') or ''
        if typ == 'string':
            return
        try:
            value = json.loads(text)
            if not isinstance(value, TYPES[typ]) or (typ in ('integer', 'number') and isinstance(value, bool)):
                error(f'wrong {typ} literal: {text[:80]}')
            if value is None:
                error('null literal is forbidden')
        except (ValueError, KeyError):
            error(f'invalid {typ} literal: {text[:80]}')

    available = set()
    for pair in task.findall('./parameters/parameter'):
        declare(pair, available)
        literal(pair)

    def steps(container, incoming, parallel=False):
        current = set(incoming)
        children = sorted(container.findall('taskStep'), key=lambda e: int(e.get('order')))
        writes = set()
        for step in children:
            before = set(incoming if parallel else current)
            cond = step.get('ifParameter')
            if cond:
                require(cond, before, 'condition')
            if step.get('ifOperator') == 'equals':
                try:
                    json.loads(step.get('ifValue', ''))
                except ValueError:
                    error('equals requires JSON ifValue')
            outputs, changed = node(step, before)
            if parallel and writes & changed:
                error(f'parallel siblings write same parameters: {sorted(writes & changed)}')
            writes |= changed
            current |= outputs
        return current, writes

    def node(step, before):
        rest = step.find('rest')
        compute = step.find('compute')
        wait = step.find('wait')
        par = step.find('parallel')
        outputs = set()
        changed = set()
        if par is not None:
            after, changed = steps(par, before, parallel=True)
            return after - before, changed
        if compute is not None:
            for pair in compute.findall('parameter'):
                expression = pair.findtext('value') or ''
                for ref in re.findall(r'param\([\'\"]([^\'\"]+)[\'\"]\)', expression):
                    require(ref, before, 'compute')
                declare(pair, outputs)
                changed.add(pair.findtext('name'))
        if wait is not None:
            if bool(wait.get('milliseconds')) == bool(wait.get('parameter')):
                error('wait needs exactly one milliseconds/parameter')
            for attr in ('parameter', 'offsetParameter'):
                if wait.get(attr):
                    require(wait.get(attr), before, 'wait')
        if rest is None:
            return outputs, changed
        auth = rest.find('auth')
        if auth is None:
            error('REST requires explicit auth policy')
        elif auth.get('mode') == 'bearer':
            require(auth.get('parameter'), before, 'auth')
            if declared.get(auth.get('parameter')) != 'string':
                error('bearer parameter must be string')
        header_names = [(h.findtext('name') or '').lower() for h in rest.findall('header')]
        if len(header_names) != len(set(header_names)):
            error('duplicate HTTP headers')
        auth_headers = header_names.count('authorization')
        if auth is not None and ((auth.get('mode') == 'header' and auth_headers != 1)
                                 or (auth.get('mode') != 'header' and auth_headers)):
            error('Authorization header conflicts with auth mode')
        for raw, bound in [('rawPath', 'pathParams'), ('rawQuery', 'queryParams'), ('rawBody', 'body')]:
            if rest.find(raw) is not None and rest.find(bound) is not None:
                error(f'{raw}/{bound} are mutually exclusive')
        for box in ('pathParams', 'queryParams', 'body'):
            keys = []
            for kv in rest.findall(f'{box}/kv'):
                ref = kv.findtext('parameter')
                key = kv.findtext('key') or ref
                if not key:
                    error(f'{box} literal requires key')
                keys.append(key)
                if ref:
                    require(ref, before, box)
                else:
                    value = kv.findtext('value') or ''
                    match = re.fullmatch(r'\{([^{}]+)\}', value)
                    if match:
                        require(match.group(1), before, box)
                    else:
                        literal(kv)
                if '{{' in (kv.findtext('value') or ''):
                    error(f'{box}: unresolved template')
            if len(keys) != len(set(keys)):
                error(f'{box}: duplicate bindings')
        for header in rest.findall('header'):
            value = header.findtext('value') or ''
            match = re.fullmatch(r'\{([^{}]+)\}', value)
            if match:
                require(match.group(1), before, 'header')
            elif '{{' in value:
                error('unresolved header template')
        for raw in ('rawBody', 'rawPath', 'rawQuery'):
            if '{{' in (rest.findtext(raw) or ''):
                error(f'{raw}: unresolved template (raw values do not interpolate)')
        code = rest.findtext('assertionCode')
        if code is None:
            error('REST requires explicit assertion')
        else:
            try:
                tree = ast.parse(code)
                compile(tree, str(path), 'exec')
                for call in ast.walk(tree):
                    if (isinstance(call, ast.Call) and isinstance(call.func, ast.Name)
                            and hasattr(builtins, call.func.id)
                            and call.func.id not in ASSERTION_BUILTINS):
                        error(f'assertion sandbox does not provide {call.func.id}')
                    if isinstance(call, ast.Name) and call.id.startswith('_'):
                        error(f'assertion sandbox forbids private name {call.id}')
                fns = [n for n in tree.body if isinstance(n, ast.FunctionDef) and n.name == 'assert_response']
                if len(fns) != 1 or [a.arg for a in fns[0].args.args] != ['responseBody', 'globalParam', 'statusCode', 'headers']:
                    error('assert_response must have the four documented arguments')
                for n in ast.walk(tree):
                    if (isinstance(n, ast.Subscript) and isinstance(n.value, ast.Name)
                        and n.value.id == 'globalParam' and isinstance(n.slice, ast.Constant)
                        and isinstance(n.slice.value, str)):
                        require(n.slice.value, before, 'assertion')
                    if (isinstance(n, ast.Call) and isinstance(n.func, ast.Attribute)
                        and isinstance(n.func.value, ast.Name) and n.func.value.id == 'globalParam'
                        and n.func.attr == 'get' and n.args and isinstance(n.args[0], ast.Constant)):
                        require(n.args[0].value, before, 'assertion')
            except (SyntaxError, ValueError) as exc:
                error(f'assertion syntax: {exc}')
        for pair in rest.findall('extractParam'):
            declare(pair, outputs)
            changed.add(pair.findtext('name'))
        for pair in rest.findall('updateParam'):
            name = pair.findtext('name')
            require(name, before, 'updateParam')
            changed.add(name)
        return outputs, changed

    available, _ = steps(task, available)
    cleanup = task.find('finally')
    if cleanup is not None:
        steps(cleanup, available)
    validate_api(task, declared, error)
    return errors


def validate_api(task, declared, error):
    """Validate request bindings against the checked-in OpenAPI, without HTTP."""
    import yaml
    from jsonschema import Draft7Validator, FormatChecker
    apis = []
    for file in sorted((ROOT.parent.parent / 'src/main/resources/rest-api').glob('*.yaml')):
        apis.append(yaml.safe_load(file.read_text()))

    def resolve(value, api):
        seen = set()
        while isinstance(value, dict) and '$ref' in value:
            ref = value['$ref']
            if not ref.startswith('#/') or ref in seen:
                raise ValueError(f'unsupported/circular OpenAPI reference {ref}')
            seen.add(ref)
            value = api
            for part in ref[2:].split('/'):
                value = value[part]
        return value

    def expand(value, api):
        value = resolve(value, api)
        if isinstance(value, dict):
            return {k: expand(v, api) for k, v in value.items()}
        if isinstance(value, list):
            return [expand(v, api) for v in value]
        return value

    fixed = {}
    for p in task.findall('./parameters/parameter'):
        try:
            fixed[p.findtext('name')] = (p.findtext('value') or '') if p.get('valueType', 'string') == 'string' else json.loads(p.findtext('value'))
        except ValueError:
            pass

    def field(schema, key):
        if not key.startswith('/'):
            return schema.get('properties', {}).get(key)
        value = schema
        for part in key[1:].split('/'):
            part = part.replace('~1', '/').replace('~0', '~')
            if value.get('type') == 'array':
                if not part.isdigit():
                    return None
                value = value.get('items', {})
            else:
                value = value.get('properties', {}).get(part)
            if value is None:
                return None
        return value

    for rest in task.findall('.//rest'):
        method, path = rest.get('method'), rest.get('path')
        match = [(api, api['paths'][path]) for api in apis if path in api.get('paths', {}) and method.lower() in api['paths'][path]]
        if len(match) != 1:
            error(f'OpenAPI: {method} {path} matched {len(match)} operations')
            continue
        api, path_item = match[0]
        op = path_item[method.lower()]
        params = [resolve(p, api) for p in path_item.get('parameters', []) + op.get('parameters', [])]
        request = resolve(op.get('requestBody', {}), api)
        body_schema = expand(request.get('content', {}).get('application/json', {}).get('schema', {}), api)
        for location, box, raw in [('path', 'pathParams', 'rawPath'), ('query', 'queryParams', 'rawQuery'), ('body', 'body', 'rawBody')]:
            if rest.find(raw) is not None:
                continue
            bindings = rest.findall(f'{box}/kv')
            bound_keys = set()
            schema = body_schema if location == 'body' else {'type': 'object', 'properties': {p['name']: expand(p.get('schema', {}), api) for p in params if p.get('in') == location}}
            for kv in bindings:
                key = kv.findtext('key') or kv.findtext('parameter') or ''
                bound_keys.add(key)
                target = field(schema, key)
                if target is None:
                    error(f'OpenAPI {method} {path}: unknown {box} field {key}')
                    continue
                ref = kv.findtext('parameter')
                value = kv.findtext('value') or ''
                alias = re.fullmatch(r'\{([^{}]+)\}', value)
                if alias:
                    ref = alias.group(1)
                if ref and ref not in fixed:
                    actual = declared.get(ref)
                    expected = target.get('type')
                    if actual and expected and actual != expected and not (actual == 'integer' and expected == 'number'):
                        error(f'OpenAPI {method} {path}: {key} expects {expected}, parameter {ref} is {actual}')
                    continue
                try:
                    v = fixed[ref] if ref else (value if kv.get('valueType', 'string') == 'string' else json.loads(value))
                    for problem in Draft7Validator(target, format_checker=FormatChecker()).iter_errors(v):
                        error(f'OpenAPI {method} {path} {box}.{key}: {problem.message}')
                except (ValueError, KeyError):
                    pass
            required = schema.get('required', []) if location == 'body' else [p['name'] for p in params if p.get('in') == location and p.get('required')]
            for key in required:
                if key not in bound_keys and '/' + key not in bound_keys and not any(k.startswith('/' + key + '/') for k in bound_keys):
                    error(f'OpenAPI {method} {path}: missing required {box}.{key}; negative requests require {raw}')
            if location == 'body' and request.get('required') and rest.find('body') is None:
                error(f'OpenAPI {method} {path}: missing required request body')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('files', nargs='*', type=Path)
    parser.add_argument('--partial', action='store_true', help='allow missing not-yet-generated cases')
    args = parser.parse_args()
    schema = etree.XMLSchema(etree.parse(str(ROOT / 'task.xsd')))
    paths = args.files or sorted(ROOT.glob('[0-9][0-9][0-9]-*.xml'))
    errors = []
    if not args.files and not args.partial:
        for src in sorted(SOURCE.glob('[0-9][0-9][0-9]-*.md')):
            if not (ROOT / (src.stem + '.xml')).is_file():
                errors.append(f'missing XML: {src.stem}')
            if not (ROOT / 'notes' / (src.stem[:3] + '.md')).is_file():
                errors.append(f'missing notes: {src.stem[:3]}')
    names = set()
    for path in paths:
        errors.extend(validate(path, schema))
        try:
            name = etree.parse(str(path)).getroot().get('name')
            if name in names:
                errors.append(f'duplicate task name: {name}')
            names.add(name)
        except etree.XMLSyntaxError:
            pass
    for error in errors:
        print(error)
    print(f'{len(paths)} XML files checked; {len(errors)} error(s).')
    raise SystemExit(bool(errors))


if __name__ == '__main__':
    main()
