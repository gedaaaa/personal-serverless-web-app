<script>
  export let data;
  const posts = data.posts; // 从服务端获取的文章列表
</script>

<div class="container mx-auto flex flex-col items-center px-4">
  <div class="flex w-full max-w-3xl flex-col items-start justify-center">
    <h1 class="mb-6 text-4xl font-bold">PLAYGROUND</h1>
    <p class="mb-6 text-lg text-gray-700">
      This is a playground for testing and experimenting with new ideas. See
      items below.
    </p>
    <ul class="w-full">
      {#each posts as post}
        <li class="my-4 text-left">
          <a
            class="group my-4 block w-full rounded-md px-4 py-4 shadow-md duration-1000 hover:shadow-lg hover:shadow-purple-400/50"
            href="/playground/{post.slug}"
          >
            <h2
              class="mb-4 text-lg font-bold duration-1000 group-hover:text-purple-600"
            >
              {post.title}
            </h2>
            <p class="mb-2 text-sm text-gray-500">
              {post.description}
            </p>
            <p class="text-sm text-gray-500">
              Date: <time datetime={post.date}>
                {new Date(post.date).toLocaleDateString('en-US', {
                  year: 'numeric',
                  month: 'long',
                  day: 'numeric',
                })}
              </time>
            </p>
          </a>
        </li>
      {/each}
    </ul>
  </div>
</div>
