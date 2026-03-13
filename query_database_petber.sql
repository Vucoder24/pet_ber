create table users (
id uuid primary key default uuid_generate_v4(),
email text unique,
password_hash text,
username text,
full_name text,
avatar_url text,
bio text,
phone text,
created_at timestamp default now(),
updated_at timestamp
);

create table pets (
id uuid primary key default uuid_generate_v4(),
owner_id uuid references users(id),
avatar_url text,
name text,
breed text,
species text,
age int,
location text,
description text,
health_status text,
created_at timestamp default now(),
updated_at timestamp
);

create table pet_images (
id uuid primary key default uuid_generate_v4(),
pet_id uuid references pets(id),
image_url text,
created_at timestamp default now()
);

create table posts (
id uuid primary key default uuid_generate_v4(),
user_id uuid references users(id),
pet_id uuid references pets(id),
caption text,
location text,
hashtags text,
like_count int default 0,
comment_count int default 0,
created_at timestamp default now(),
updated_at timestamp
);

create table post_media (
id uuid primary key default uuid_generate_v4(),
post_id uuid references posts(id),
media_url text,
media_type text,
created_at timestamp default now()
);

create table comments (
id uuid primary key default uuid_generate_v4(),
post_id uuid references posts(id),
user_id uuid references users(id),
content text,
created_at timestamp default now()
);

create table post_likes (
id uuid primary key default uuid_generate_v4(),
post_id uuid references posts(id),
user_id uuid references users(id),
created_at timestamp default now()
);

create table follows (
id uuid primary key default uuid_generate_v4(),
follower_id uuid references users(id),
following_id uuid references users(id),
created_at timestamp default now()
);

create table pet_follows (
id uuid primary key default uuid_generate_v4(),
user_id uuid references users(id),
pet_id uuid references pets(id),
created_at timestamp default now()
);

create table notifications (
id uuid primary key default uuid_generate_v4(),
user_id uuid references users(id),
sender_id uuid references users(id),
type text,
post_id uuid,
comment_id uuid,
is_read boolean default false,
created_at timestamp default now()
);

create table conversations (
id uuid primary key default uuid_generate_v4(),
created_at timestamp default now()
);

create table messages (
id uuid primary key default uuid_generate_v4(),
conversation_id uuid references conversations(id),
sender_id uuid references users(id),
message_text text,
created_at timestamp default now()
);

create table hashtags (
id uuid primary key default uuid_generate_v4(),
name text,
created_at timestamp default now()
);

create table post_hashtags (
id uuid primary key default uuid_generate_v4(),
post_id uuid references posts(id),
hashtag_id uuid references hashtags(id)
);

create table stories (
id uuid primary key default uuid_generate_v4(),
user_id uuid references users(id),
image_url text,
created_at timestamp default now(),
expires_at timestamp
);

-- tạo function chèn dữ liệu khi đăng kí vào bảng users
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
as $$
begin
  insert into public.users (
    id,
    email,
    username,
    created_at
  )
  values (
    new.id,
    new.email,
    new.raw_user_meta_data->>'user_name',
    now()
  );

  return new;
end;
$$;

-- tạo trigger chèn thông tin vào users
create trigger on_auth_user_created
after insert on auth.users
for each row
execute procedure public.handle_new_user();

-- Cho phép đọc bảng stories
ALTER TABLE stories ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow public read access" ON stories FOR SELECT USING (true);

-- Đảm bảo bảng users cũng cho phép đọc (vì bạn có join users)
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Allow public read access" ON users FOR SELECT USING (true);


-- 1. Đảm bảo Extension UUID hoạt động (thường đã có sẵn trên Supabase)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Cập nhật hàm Tăng/Giảm Like (Thêm search_path để bảo mật)
CREATE OR REPLACE FUNCTION public.increment_like_count(p_post_id uuid)
RETURNS void AS $$
BEGIN
  UPDATE public.posts
  SET like_count = like_count + 1
  WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = '';

CREATE OR REPLACE FUNCTION public.decrement_like_count(p_post_id uuid)
RETURNS void AS $$
BEGIN
  UPDATE public.posts
  SET like_count = GREATEST(like_count - 1, 0)
  WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = '';

-- 3. Ràng buộc (Constraints) - Ngăn chặn dữ liệu rác
-- Xóa nếu đã tồn tại để tránh lỗi khi chạy lại script
ALTER TABLE public.post_likes DROP CONSTRAINT IF EXISTS unique_post_like;
ALTER TABLE public.post_likes ADD CONSTRAINT unique_post_like UNIQUE (post_id, user_id);

ALTER TABLE public.follows DROP CONSTRAINT IF EXISTS unique_follow;
ALTER TABLE public.follows ADD CONSTRAINT unique_follow UNIQUE (follower_id, following_id);

-- 4. Cấu hình RLS (Row Level Security)
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.post_likes ENABLE ROW LEVEL SECURITY;

-- Policies cho bảng Users
CREATE POLICY "Users are viewable by everyone" ON public.users FOR SELECT USING (true);
CREATE POLICY "Users can update own profile" ON public.users FOR UPDATE USING (auth.uid() = id);

-- -- Policies cho bảng Posts
-- CREATE POLICY "Posts are viewable by everyone" ON public.posts FOR SELECT USING (true);
-- CREATE POLICY "Users can insert own posts" ON public.posts FOR INSERT WITH CHECK (auth.uid() = user_id);
-- CREATE POLICY "Users can update own posts" ON public.posts FOR UPDATE USING (auth.uid() = user_id);

-- -- Policies cho bảng Likes
-- CREATE POLICY "Users can manage own likes" ON public.post_likes FOR ALL USING (auth.uid() = user_id);
-- create policy "insert post like"
-- on public.post_likes
-- for insert
-- to authenticated
-- with check (auth.uid() = user_id);

-- 5. Hàm tự động đồng bộ User từ Auth sang Public (Đã tối ưu)
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.users (id, email, username, avatar_url, created_at)
  VALUES (
    new.id,
    new.email,
    COALESCE(new.raw_user_meta_data->>'user_name', new.raw_user_meta_data->>'full_name', 'user_' || substr(new.id::text, 1, 8)),
    new.raw_user_meta_data->>'avatar_url',
    now()
  )
  ON CONFLICT (id) DO UPDATE
  SET email = EXCLUDED.email,
      username = EXCLUDED.username;
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger (Xóa trigger cũ trước khi tạo mới để tránh lỗi)
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- Danh sách các bảng nội dung công khai
ALTER TABLE pets ENABLE ROW LEVEL SECURITY;
ALTER TABLE pet_images ENABLE ROW LEVEL SECURITY;
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE hashtags ENABLE ROW LEVEL SECURITY;
ALTER TABLE post_hashtags ENABLE ROW LEVEL SECURITY;
ALTER TABLE stories ENABLE ROW LEVEL SECURITY;

-- Tạo chính sách cho phép đọc (SELECT) cho tất cả mọi người
CREATE POLICY "Allow public read" ON pets FOR SELECT USING (true);
CREATE POLICY "Allow public read" ON pet_images FOR SELECT USING (true);
CREATE POLICY "Allow public read" ON posts FOR SELECT USING (true);
CREATE POLICY "Allow public read" ON post_media FOR SELECT USING (true);
CREATE POLICY "Allow public read" ON comments FOR SELECT USING (true);
CREATE POLICY "Allow public read" ON post_likes FOR SELECT USING (true);
CREATE POLICY "Allow public read" ON hashtags FOR SELECT USING (true);
CREATE POLICY "Allow public read" ON post_hashtags FOR SELECT USING (true);
CREATE POLICY "Allow public read" ON stories FOR SELECT USING (true);