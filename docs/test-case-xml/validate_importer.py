"""Offline adapter check against a local AutoTestService source tree; no DB or HTTP.

API records are built in memory from checked-in OpenAPI. This intentionally tests
XML conversion and task validation separately from uploading API definitions.
"""
import argparse
import sys
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--importer-root',required=True,type=Path)
parser.add_argument('files',nargs='*',type=Path)
args=parser.parse_args()
sys.path.insert(0,str(args.importer_root.resolve()))
from app.service import openapi_import,task_import,task_config
from app.service.errors import BusinessError
root=Path(__file__).resolve().parents[2]
lookup_map={}
import yaml
from copy import deepcopy
for j,p in enumerate(sorted((root/'src/main/resources/rest-api').glob('*.yaml'))):
    document=yaml.safe_load(p.read_text())
    def expand(value):
        if isinstance(value,dict):
            if '$ref' in value:
                target=document
                for part in value['$ref'][2:].split('/'): target=target[part]
                return expand(target)
            return {k:expand(v) for k,v in value.items()}
        if isinstance(value,list): return [expand(v) for v in value]
        return value
    for path,item in document['paths'].items():
        for method,op in item.items():
            if method not in ('get','post','patch','delete','put','head','options'): continue
            i={'file_id':str(j),'id':f'{j}-{len(lookup_map)}','version':1,'method':method.upper(),'path':path,'parameters':[]}
            for v in item.get('parameters',[])+op.get('parameters',[]):
                v=expand(v);i['parameters'].append({'name':v['name'],'location':v['in'],'required':v.get('required',False),'schema':v.get('schema',{})})
            if 'requestBody' in op:
                body=expand(op['requestBody']); content_type,media=next(iter(body['content'].items()))
                i['request_body']={'required':body.get('required',False),'content_type':content_type,'schema':media.get('schema',{})}
            lookup_map[(i['method'],path)]=(i,document)
def lookup(method,path): return lookup_map[(method,path)]
errors=[]
paths=args.files or sorted((root/'docs/test-case-xml').glob('[0-9][0-9][0-9]-*.xml'))
for p in paths:
    try:
        tree=task_import.parse_upload(p.name,p.read_bytes())
        config=task_import.convert(tree,'http://127.0.0.1:10201/api/v1',lookup)
        for index,s in enumerate(config['steps']):
            s['id']=str(index)
            if 'parent_step_index' in s: s['parent_step_id']=str(s.pop('parent_step_index'))
            if s['step_type']=='rest':
                s['interface'],s['document']=next(v for v in lookup_map.values() if v[0]['id']==s['interface_id'])
        for param in config['global_parameters']:
            for key in ('rest_source','expression_source'):
                if key in param: param[key]['step_id']=str(param[key].pop('step_index'))
        task_config.validate_config(config,run=True)
    except BusinessError as e:
        errors.append(f'{p.name}: {e.payload}')
for e in errors: print(e)
print(f'actual importer offline validation: {len(paths)} XML, {len(errors)} errors')
sys.exit(bool(errors))
