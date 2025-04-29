// +server.js

import satori from 'satori';
import { Resvg } from '@resvg/resvg-js';
import { html as toReactElement } from 'satori-html';
import { error } from '@sveltejs/kit';
import defaultMetadata from '$lib/seo/defaultMetadata.js';

import fontDataURI from './font-og.woff?inline';

// OG image size
const height = 630;
const width = 1200;

export const GET = async ({ url }) => {
  let metadata;

  const isHomePage = url.searchParams.get('isHomePage');

  // set default metadata for home page
  if (isHomePage && url.searchParams.size === 1) {
    metadata = defaultMetadata;
  }

  // set metadata for post page
  const category = url.searchParams.get('category');
  const slug = url.searchParams.get('slug');
  if (category && slug && url.searchParams.size === 2) {
    try {
      // Load the content
      let post;
      try {
        post = await import(`../${category}/markdowns/${slug}.svx`);
      } catch {
        post = await import(`../${category}/markdowns/${slug}.md`);
      }
      metadata = post.metadata;
    } catch {
      throw error(404, 'Not found');
    }
  }

  if (!metadata) {
    throw error(404, 'Not found');
  }

  const template = `<html>
      <style>
        @import url('https://fonts.googleapis.com/css2?family=Inter:wght@700&display=swap');
        body {
          margin: 0;
          padding: 4rem;
          background: linear-gradient(135deg, #ffedd5, #bef264);
          height: 100vh;
          width: 100vw;
          color: white;
          font-family: 'Inter', sans-serif;
        }
        .container {
          height: 100%;
          width: 100%;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
        }

        h1 {
          font-size: 3.5rem;
          line-height: 1.2;
          margin: 0 0 2rem 0;
          color: #9333ea;
        }
        .meta {
          font-size: 1.5rem;
          opacity: 0.8;
          color: #374151;
          margin-top: 1rem;
        }
      </style>
      <body>
        <div class="container">
          <h1>${metadata.title || 'Welcome to this personal website'}</h1>
          <div class="meta">
            ${metadata.description || 'have fun!'}
          </div>
          <div class="meta">
            ${
              metadata.date
                ? new Date(metadata.date).toLocaleDateString('en-US', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric',
                  })
                : ''
            }
          </div>
        </div>
      </body>
    </html>`;

  // convert template to react element, satori only accepts react elements
  const html = toReactElement(template);

  // inline data URI should start with pattern like 'data:font/woff;base64,AAEAAA...'
  const base64String = fontDataURI.split(',')[1];
  const fontDataBuffer = Buffer.from(base64String, 'base64');

  // generate svg
  const svg = await satori(html, {
    height,
    width,
    fonts: [
      {
        name: 'Inter Latin',
        data: fontDataBuffer,
        style: 'normal',
      },
    ],
  });

  const resvg = new Resvg(svg, {
    fitTo: {
      mode: 'width',
      value: width,
    },
  });

  const image = resvg.render();

  return new Response(image.asPng(), {
    headers: {
      'content-type': 'image/png',
      'cache-control': 'public, max-age=604800, stale-while-revalidate=2592000',
    },
  });
};
