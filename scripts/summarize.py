"""Convert JMH's batch timings to a readable table without hiding uncertainty."""
import csv,json,math
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
records=[line.rstrip(b'\r\n') for line in (ROOT/'src/main/resources/birds.data').read_bytes().splitlines(keepends=True)]
count=len(records)
assert count==25000
input_bytes=sum(map(len,records))
results=json.loads((ROOT/'results/jmh.json').read_text())
assert len(results)==14, f'Expected 14 benchmarks, got {len(results)}'
rows=[]
for result in results:
    name=result['benchmark'];metric=result['primaryMetric']
    assert metric['scoreUnit']=='ms/op' and math.isfinite(metric['score']) and metric['score']>0
    method=name.rsplit('.',1)[1]
    operation='decode' if name.startswith('benchmarks.Deserialization') else 'encode'
    library=method.split('Marshaller')[0].lower()
    allocated=result['secondaryMetrics']['gc.alloc.rate.norm']['score']
    row=dict(library=library,operation=operation,batch_ms=metric['score'],batch_error_ms=metric['scoreError'],
        rows_per_second=count*1000/metric['score'],allocated_bytes_per_row=allocated/count,
        input_MB_per_second=(input_bytes/metric['score']/1000 if operation=='decode' else ''))
    rows.append(row)
with (ROOT/'results/summary.csv').open('w',newline='') as f:
    writer=csv.DictWriter(f,fieldnames=list(rows[0]));writer.writeheader();writer.writerows(rows)
text=['# Mini: Scala JSON benchmark', '',
    'Each JMH operation processes the complete 25,000-record fixture. Lower time is better.',
    'The ± values are JMH\'s reported 99.9% confidence intervals, not standard deviations.',
    'This is a single-thread typed case-class microbenchmark, not an HTTP, Spark or streaming-parser benchmark.',
    'Encoders retain library defaults; their JSON output is round-trip equivalent but can differ in formatting and optional fields.', '',
    '| Library | Decode batch ms | Encode batch ms | Decode rows/s | Decode input MB/s | Decode allocation B/row |',
    '|---|---:|---:|---:|---:|---:|']
for row in sorted((r for r in rows if r['operation']=='decode'),key=lambda r:r['batch_ms']):
    encode=next(r for r in rows if r['operation']=='encode' and r['library']==row['library'])
    text.append(f"| {row['library']} | {row['batch_ms']:.3f} ± {row['batch_error_ms']:.3f} | {encode['batch_ms']:.3f} ± {encode['batch_error_ms']:.3f} | {row['rows_per_second']:,.0f} | {row['input_MB_per_second']:.1f} | {row['allocated_bytes_per_row']:,.0f} |")
text+=['',f'Fixture: {count:,} JSON records, {input_bytes:,} UTF-8 bytes excluding line separators.',
       'Raw timings, all allocation metrics and per-fork samples: `jmh.json`. Full run output: `jmh.log`.',
       'CPU/JVM and fixture checksum: `environment.txt`; source checksums: `source.sha256`.']
(ROOT/'results/README.md').write_text('\n'.join(text)+'\n')
print('\n'.join(text))
